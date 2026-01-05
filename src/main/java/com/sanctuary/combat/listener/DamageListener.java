package com.sanctuary.combat.listener;

import com.sanctuary.combat.calc.DamageCalculator;
import com.sanctuary.combat.calc.DefenseCalculator;
import com.sanctuary.combat.event.CombatContext;
import com.sanctuary.combat.event.CombatEventBus;
import com.sanctuary.combat.event.CombatEventType;
import com.sanctuary.combat.event.DamageDealtEvent;
import com.sanctuary.combat.model.DamageContext;
import com.sanctuary.combat.stat.AttributeContainer;
import com.sanctuary.combat.stat.StatManager;
import com.sanctuary.combat.status.StatusEffectManager;
import com.sanctuary.core.ecs.SanctuaryEntity;
import com.sanctuary.core.script.ScriptEngine;
import com.sanctuary.items.model.RpgItemData;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Location;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.scheduler.BukkitRunnable;
import org.luaj.vm2.LuaTable;
import org.luaj.vm2.LuaValue;

import java.util.function.Supplier;

/**
 * DamageListener
 * 마인크래프트의 데미지 이벤트를 가로채서 커스텀 로직을 수행합니다.
 * 유니크 아이템 onHit/onKill 스크립트도 실행합니다.
 */
public class DamageListener implements Listener {

    private final StatManager statManager;
    private final DamageCalculator damageCalculator;
    private final DefenseCalculator defenseCalculator;
    private final StatusEffectManager statusEffectManager;
    private final CombatEventBus eventBus;

    // 유니크 아이템 훅 지원
    private ScriptEngine scriptEngine;
    private Supplier<RpgItemData> equippedWeaponSupplier;

    /**
     * 레거시 생성자 (이벤트 버스 없이)
     */
    public DamageListener(StatManager statManager, DamageCalculator damageCalculator,
            DefenseCalculator defenseCalculator, StatusEffectManager statusEffectManager) {
        this(statManager, damageCalculator, defenseCalculator, statusEffectManager, null);
    }

    /**
     * 새 생성자 (이벤트 버스 포함)
     */
    public DamageListener(StatManager statManager, DamageCalculator damageCalculator,
            DefenseCalculator defenseCalculator, StatusEffectManager statusEffectManager,
            CombatEventBus eventBus) {
        this.statManager = statManager;
        this.damageCalculator = damageCalculator;
        this.defenseCalculator = defenseCalculator;
        this.statusEffectManager = statusEffectManager;
        this.eventBus = eventBus;
    }

    /**
     * 유니크 아이템 스크립트 지원을 설정합니다.
     */
    public void setScriptEngine(ScriptEngine scriptEngine) {
        this.scriptEngine = scriptEngine;
    }

    /**
     * 장착 무기 공급자를 설정합니다.
     */
    public void setEquippedWeaponSupplier(Supplier<RpgItemData> supplier) {
        this.equippedWeaponSupplier = supplier;
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onEntityDamage(EntityDamageByEntityEvent event) {
        if (!(event.getDamager() instanceof LivingEntity attacker)
                || !(event.getEntity() instanceof LivingEntity victim)) {
            return;
        }

        // 1. 바닐라 데미지 무효화
        event.setDamage(0);

        // 2. 스탯 데이터 가져오기
        AttributeContainer attackerStats = statManager.getStats(attacker);
        AttributeContainer victimStats = statManager.getStats(victim);

        // 3. 컨텍스트 생성
        DamageContext ctx = new DamageContext(attacker, victim, attackerStats, victimStats);
        ctx.addTag("PHYSICAL"); // 기본 물리 공격

        // 상태 이상 체크: 취약 적용
        if (statusEffectManager.isVulnerable(victim)) {
            ctx.setVulnerable(true);
        }

        // 4. 공격 데미지 계산 (Attack Phase)
        double rawDamage = damageCalculator.calculate(ctx);

        // 5. 방어 계산 (Defense Phase)
        double mitigatedDamage = defenseCalculator.applyDefense(ctx, rawDamage);

        // 5.1 보강(Fortify) 적용
        double fortifyAmount = statusEffectManager.getFortifyAmount(victim);
        if (fortifyAmount > 0) {
            mitigatedDamage = defenseCalculator.applyFortify(mitigatedDamage, victim.getHealth(), fortifyAmount);
        }

        // 5.2 보호막(Barrier) 적용
        SanctuaryEntity victimEntity = statManager.getCore().getEntityManager().get(victim.getUniqueId());
        if (victimEntity != null) {
            com.sanctuary.core.ecs.component.StateComponent stateComp = victimEntity
                    .getComponent(com.sanctuary.core.ecs.component.StateComponent.class);
            if (stateComp != null && stateComp.hasBarrier()) {
                mitigatedDamage = stateComp.damageBarrier(mitigatedDamage);
            }
        }

        // 6. 유니크 아이템 onHit 스크립트 실행
        if (attacker instanceof Player player) {
            mitigatedDamage = executeOnHitScript(player, victim, mitigatedDamage, ctx);
        }

        // 7. CombatEventBus로 이벤트 발생
        if (eventBus != null) {
            CombatContext combatCtx = CombatContext.builder()
                    .attacker(statManager.getCore().getEntityManager().get(attacker.getUniqueId()))
                    .victim(victimEntity)
                    .skillCoefficient(1.0)
                    .build();

            DamageDealtEvent damageEvent = new DamageDealtEvent(combatCtx, rawDamage)
                    .finalDamage(mitigatedDamage)
                    .damageType("PHYSICAL")
                    .critical(ctx.isCritical())
                    .overpower(ctx.isOverpower())
                    .vulnerable(ctx.isVulnerable());

            boolean cancelled = eventBus.fire(damageEvent);
            if (cancelled) {
                return; // 이벤트가 취소됨
            }
            mitigatedDamage = damageEvent.getFinalDamage();
        }

        // 8. 최종 데미지 적용
        double currentHealth = victim.getHealth();
        double newHealth = Math.max(0, currentHealth - mitigatedDamage);
        victim.setHealth(newHealth);

        // 9. 결과 출력 (데미지 인디케이터)
        showDamageIndicator(victim, mitigatedDamage, ctx);

        // 디버그 메시지 (플레이어에게만)
        if (attacker instanceof Player player) {
            sendDebugMessage(player, rawDamage, mitigatedDamage, ctx);
        }
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onEntityDeath(EntityDeathEvent event) {
        LivingEntity victim = event.getEntity();
        Player killer = victim.getKiller();

        if (killer == null) {
            return;
        }

        // 유니크 아이템 onKill 스크립트 실행
        executeOnKillScript(killer, victim);

        // CombatEventBus로 킬 이벤트 발생
        if (eventBus != null) {
            SanctuaryEntity killerEntity = statManager.getCore().getEntityManager().get(killer.getUniqueId());
            SanctuaryEntity victimEntity = statManager.getCore().getEntityManager().get(victim.getUniqueId());

            CombatContext ctx = CombatContext.builder()
                    .attacker(killerEntity)
                    .victim(victimEntity)
                    .build();

            // ENTITY_KILLED 이벤트 발생 (필요시 구현)
        }
    }

    /**
     * 유니크 아이템 onHit 스크립트를 실행합니다.
     */
    private double executeOnHitScript(Player attacker, LivingEntity victim, double damage, DamageContext ctx) {
        if (scriptEngine == null || equippedWeaponSupplier == null) {
            return damage;
        }

        RpgItemData weapon = equippedWeaponSupplier.get();
        if (weapon == null || weapon.getOnHitScript() == null) {
            return damage;
        }

        try {
            LuaTable context = new LuaTable();

            // 플레이어 정보
            LuaTable playerTable = new LuaTable();
            playerTable.set("uuid", attacker.getUniqueId().toString());
            playerTable.set("name", attacker.getName());
            context.set("player", playerTable);

            // 대상 정보
            LuaTable targetTable = new LuaTable();
            targetTable.set("uuid", victim.getUniqueId().toString());
            targetTable.set("type", victim.getType().name());
            targetTable.set("health", victim.getHealth());
            context.set("target", targetTable);

            // 피해 정보
            context.set("damage", damage);
            context.set("isCrit", LuaValue.valueOf(ctx.isCritical()));
            context.set("isOverpower", LuaValue.valueOf(ctx.isOverpower()));

            LuaValue result = scriptEngine.callFunction(weapon.getOnHitScript(), context);

            // 반환값이 숫자면 피해량 수정
            if (result != null && result.isnumber()) {
                return result.todouble();
            }
        } catch (Exception e) {
            // 스크립트 오류 무시
        }

        return damage;
    }

    /**
     * 유니크 아이템 onKill 스크립트를 실행합니다.
     */
    private void executeOnKillScript(Player killer, LivingEntity victim) {
        if (scriptEngine == null || equippedWeaponSupplier == null) {
            return;
        }

        RpgItemData weapon = equippedWeaponSupplier.get();
        if (weapon == null || weapon.getOnKillScript() == null) {
            return;
        }

        try {
            LuaTable context = new LuaTable();

            // 플레이어 정보
            LuaTable playerTable = new LuaTable();
            playerTable.set("uuid", killer.getUniqueId().toString());
            playerTable.set("name", killer.getName());
            context.set("player", playerTable);

            // 대상 정보
            LuaTable targetTable = new LuaTable();
            targetTable.set("uuid", victim.getUniqueId().toString());
            targetTable.set("type", victim.getType().name());
            context.set("target", targetTable);

            scriptEngine.callFunction(weapon.getOnKillScript(), context);
        } catch (Exception e) {
            // 스크립트 오류 무시
        }
    }

    /**
     * 데미지 인디케이터를 표시합니다.
     */
    private void showDamageIndicator(LivingEntity victim, double damage, DamageContext ctx) {
        Location loc = victim.getLocation().add(
                (Math.random() - 0.5) * 0.5,
                victim.getHeight() + 0.5 + Math.random() * 0.3,
                (Math.random() - 0.5) * 0.5);

        NamedTextColor color = NamedTextColor.WHITE;
        String suffix = "";

        if (ctx.isCritical() && ctx.isOverpower()) {
            color = NamedTextColor.GOLD;
            suffix = "!";
        } else if (ctx.isCritical()) {
            color = NamedTextColor.YELLOW;
            suffix = "!";
        } else if (ctx.isOverpower()) {
            color = NamedTextColor.AQUA;
            suffix = "☆";
        } else if (ctx.isVulnerable()) {
            color = NamedTextColor.LIGHT_PURPLE;
        }

        String damageText = String.format("%.0f%s", damage, suffix);
        Component displayName = Component.text(damageText, color);

        ArmorStand hologram = (ArmorStand) victim.getWorld().spawnEntity(loc, EntityType.ARMOR_STAND);
        hologram.setVisible(false);
        hologram.setGravity(false);
        hologram.setMarker(true);
        hologram.setCustomNameVisible(true);
        hologram.customName(displayName);

        new BukkitRunnable() {
            @Override
            public void run() {
                if (hologram.isValid()) {
                    hologram.remove();
                }
            }
        }.runTaskLater(statManager.getCore().getPlugin(), 16L);
    }

    private void sendDebugMessage(Player player, double rawDamage, double finalDamage, DamageContext ctx) {
        StringBuilder msg = new StringBuilder();
        msg.append("§7[데미지] Raw: §f").append(String.format("%.1f", rawDamage));
        msg.append(" §7→ Final: §f").append(String.format("%.1f", finalDamage));

        if (ctx.isCritical())
            msg.append(" §e(치명타!)");
        if (ctx.isOverpower())
            msg.append(" §b(제압!)");
        if (ctx.isVulnerable())
            msg.append(" §d(취약)");

        player.sendMessage(msg.toString());
    }
}
