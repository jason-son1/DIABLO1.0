package com.sanctuary.combat.listener;

import com.sanctuary.combat.calc.DamageCalculator;
import com.sanctuary.combat.calc.DefenseCalculator;
import com.sanctuary.combat.model.DamageContext;
import com.sanctuary.combat.stat.AttributeContainer;
import com.sanctuary.combat.stat.Stat;
import com.sanctuary.combat.stat.StatManager;
import com.sanctuary.combat.status.StatusEffectManager;
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
import org.bukkit.scheduler.BukkitRunnable;

/**
 * DamageListener
 * 마인크래프트의 데미지 이벤트를 가로채서 커스텀 로직을 수행합니다.
 */
public class DamageListener implements Listener {

    private final StatManager statManager;
    private final DamageCalculator damageCalculator;
    private final DefenseCalculator defenseCalculator;
    private final StatusEffectManager statusEffectManager;

    public DamageListener(StatManager statManager, DamageCalculator damageCalculator,
            DefenseCalculator defenseCalculator, StatusEffectManager statusEffectManager) {
        this.statManager = statManager;
        this.damageCalculator = damageCalculator;
        this.defenseCalculator = defenseCalculator;
        this.statusEffectManager = statusEffectManager;
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
        com.sanctuary.core.ecs.SanctuaryEntity victimEntity = statManager.getCore().getEntityManager()
                .get(victim.getUniqueId());
        if (victimEntity != null) {
            com.sanctuary.core.ecs.component.StateComponent stateComp = victimEntity
                    .getComponent(com.sanctuary.core.ecs.component.StateComponent.class);
            if (stateComp != null && stateComp.hasBarrier()) {
                mitigatedDamage = stateComp.damageBarrier(mitigatedDamage);
            }
        }

        // 6. 최종 데미지 적용
        double currentHealth = victim.getHealth();
        double newHealth = Math.max(0, currentHealth - mitigatedDamage);
        victim.setHealth(newHealth);

        // 7. 결과 출력 (데미지 인디케이터)
        showDamageIndicator(victim, mitigatedDamage, ctx);

        // 디버그 메시지 (플레이어에게만)
        if (attacker instanceof Player player) {
            sendDebugMessage(player, rawDamage, mitigatedDamage, ctx);
        }
    }

    /**
     * 데미지 인디케이터를 표시합니다.
     * 홀로그램 텍스트로 피해량을 띄웁니다.
     */
    private void showDamageIndicator(LivingEntity victim, double damage, DamageContext ctx) {
        // Paper/Adventure API 사용
        Location loc = victim.getLocation().add(
                (Math.random() - 0.5) * 0.5,
                victim.getHeight() + 0.5 + Math.random() * 0.3,
                (Math.random() - 0.5) * 0.5);

        // 색상 결정
        NamedTextColor color = NamedTextColor.WHITE;
        String suffix = "";

        if (ctx.isCritical() && ctx.isOverpower()) {
            color = NamedTextColor.GOLD; // 치명타 + 제압
            suffix = "!";
        } else if (ctx.isCritical()) {
            color = NamedTextColor.YELLOW; // 치명타
            suffix = "!";
        } else if (ctx.isOverpower()) {
            color = NamedTextColor.AQUA; // 제압
            suffix = "☆";
        } else if (ctx.isVulnerable()) {
            color = NamedTextColor.LIGHT_PURPLE; // 취약
        }

        String damageText = String.format("%.0f%s", damage, suffix);
        Component displayName = Component.text(damageText, color);

        // ArmorStand로 홀로그램 생성
        ArmorStand hologram = (ArmorStand) victim.getWorld().spawnEntity(loc, EntityType.ARMOR_STAND);
        hologram.setVisible(false);
        hologram.setGravity(false);
        hologram.setMarker(true);
        hologram.setCustomNameVisible(true);
        hologram.customName(displayName);

        // 0.8초 후 제거
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
