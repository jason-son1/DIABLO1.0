package com.sanctuary.combat.listener;

import com.sanctuary.combat.calc.DamageCalculator;
import com.sanctuary.combat.model.DamageContext;
import com.sanctuary.combat.stat.AttributeContainer;
import com.sanctuary.combat.stat.StatManager;
import org.bukkit.ChatColor;
import org.bukkit.entity.LivingEntity;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;

/**
 * DamageListener
 * 마인크래프트의 데미지 이벤트를 가로채서 커스텀 로직을 수행합니다.
 */
public class DamageListener implements Listener {

    private final StatManager statManager;
    private final DamageCalculator calculator;

    public DamageListener(StatManager statManager, DamageCalculator calculator) {
        this.statManager = statManager;
        this.calculator = calculator;
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onEntityDamage(EntityDamageByEntityEvent event) {
        if (!(event.getDamager() instanceof LivingEntity attacker)
                || !(event.getEntity() instanceof LivingEntity victim)) {
            return;
        }

        // 1. 바닐라 데미지 무효화 (Set to 0)
        // 이벤트를 캔슬하면 타격 애니메이션(knockback 등)이 사라질 수 있으므로, 데미지만 0으로 설정.
        event.setDamage(0);

        // 2. 스탯 데이터 가져오기
        AttributeContainer attackerStats = statManager.getStats(attacker);
        AttributeContainer victimStats = statManager.getStats(victim);

        // 3. 컨텍스트 생성 (Context Creation)
        DamageContext ctx = new DamageContext(attacker, victim, attackerStats, victimStats);

        // TODO: 무기 태그, 스킬 태그 등을 여기서 주입해야 함.
        ctx.addTag("PHYSICAL"); // 기본 물리 공격 가정

        // 4. 데미지 계산 (Calculation)
        double finalDamage = calculator.calculate(ctx);

        // 5. 최종 데미지 적용
        // 바닐라 체력을 직접 깎거나, setDamage를 다시 설정할 수도 있지만,
        // 방어력(Armor) 계산도 우리가 직접 해야 하므로 health를 직접 조작하는 것이 나을 수 있음.
        // 하지만 호환성을 위해 event.setDamage()에 최종값을 넣는 방식도 고려 가능.
        // 여기서는 "완전 제어"를 위해 health 직접 차감 방식을 선택하거나,
        // 방어 공식이 DamageCalculator에 포함되지 않았다면 event.setDamage를 사용해선 안됨.
        // 현재 DamageCalculator는 방어(Armor) 로직 전 단계임.

        // TODO: 방어력(Damage Reduction from Armor/Resist) 적용 로직 추가 필요.
        // 일단 테스트를 위해 계산된 데미지를 그대로 적용.

        double currentHealth = victim.getHealth();
        double newHealth = Math.max(0, currentHealth - finalDamage);
        victim.setHealth(newHealth);

        // 6. 결과 출력 (Debug / Indicator)
        sendDebugMessage(attacker, finalDamage, ctx);
    }

    private void sendDebugMessage(LivingEntity attacker, double damage, DamageContext ctx) {
        StringBuilder msg = new StringBuilder();
        msg.append(ChatColor.GRAY).append("Hit! Damage: ").append(ChatColor.WHITE)
                .append(String.format("%.1f", damage));

        if (ctx.isCritical())
            msg.append(ChatColor.YELLOW).append(" (Critical!)");
        if (ctx.isOverpower())
            msg.append(ChatColor.AQUA).append(" (Overpower!)");

        attacker.sendMessage(msg.toString());
    }
}
