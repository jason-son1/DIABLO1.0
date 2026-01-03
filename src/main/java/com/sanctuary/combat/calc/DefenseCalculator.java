package com.sanctuary.combat.calc;

import com.sanctuary.combat.model.DamageContext;
import com.sanctuary.combat.stat.AttributeContainer;
import com.sanctuary.combat.stat.Stat;

/**
 * 방어 계산기 (Defense Calculator)
 * 
 * 디아블로 IV의 방어력/저항 시스템을 구현합니다.
 * - 물리 피해: 방어력(Armor)으로 감소
 * - 원소 피해: 저항(Resistance)으로 감소
 */
public class DefenseCalculator {

    // 방어력 감소 최대치 (85%)
    private static final double ARMOR_CAP = 0.85;

    // 저항 감소 최대치 (70% 기본, 일부 상황에서 85%)
    private static final double RESISTANCE_CAP = 0.70;

    // 레벨당 방어력 스케일링 (높은 레벨의 적일수록 더 많은 방어력 필요)
    private static final double ARMOR_SCALING_PER_LEVEL = 10.0;

    /**
     * 최종 피해에 방어 감소를 적용합니다.
     * 
     * @param ctx            데미지 컨텍스트
     * @param incomingDamage 들어오는 피해량
     * @return 감소된 피해량
     */
    public double applyDefense(DamageContext ctx, double incomingDamage) {
        AttributeContainer victimStats = ctx.getVictimStats();

        double reduction = 0.0;

        // 물리 피해인 경우 방어력 적용
        if (ctx.hasTag("PHYSICAL")) {
            double armor = victimStats.getValue(Stat.ARMOR);
            reduction = calculateArmorReduction(armor, getAttackerLevel(ctx));
        }

        // 원소 피해인 경우 해당 저항 적용
        reduction = Math.max(reduction, calculateElementalReduction(ctx, victimStats));

        // 감소 적용
        double mitigatedDamage = incomingDamage * (1.0 - reduction);

        return Math.max(0, mitigatedDamage);
    }

    /**
     * 방어력에 의한 피해 감소율을 계산합니다.
     * 
     * 공식: Reduction = Armor / (Armor + ScalingConstant * EnemyLevel)
     * 
     * @param armor         방어력 수치
     * @param attackerLevel 공격자 레벨
     * @return 감소율 (0.0 ~ ARMOR_CAP)
     */
    public double calculateArmorReduction(double armor, int attackerLevel) {
        if (armor <= 0)
            return 0.0;

        double scalingConstant = ARMOR_SCALING_PER_LEVEL * attackerLevel;
        double reduction = armor / (armor + scalingConstant);

        return Math.min(reduction, ARMOR_CAP);
    }

    /**
     * 저항에 의한 원소 피해 감소율을 계산합니다.
     */
    private double calculateElementalReduction(DamageContext ctx, AttributeContainer stats) {
        double resistance = 0.0;

        if (ctx.hasTag("FIRE")) {
            resistance = stats.getValue(Stat.RESISTANCE_FIRE);
        } else if (ctx.hasTag("COLD")) {
            resistance = stats.getValue(Stat.RESISTANCE_COLD);
        } else if (ctx.hasTag("LIGHTNING")) {
            resistance = stats.getValue(Stat.RESISTANCE_LIGHTNING);
        } else if (ctx.hasTag("POISON")) {
            resistance = stats.getValue(Stat.RESISTANCE_POISON);
        } else if (ctx.hasTag("SHADOW")) {
            resistance = stats.getValue(Stat.RESISTANCE_SHADOW);
        }

        // 저항은 퍼센트 값으로 저장됨 (0.3 = 30%)
        return Math.min(resistance, RESISTANCE_CAP);
    }

    /**
     * 공격자 레벨을 가져옵니다.
     * TODO: ECS의 IdentityComponent에서 레벨 조회
     */
    private int getAttackerLevel(DamageContext ctx) {
        // 현재는 기본값 반환
        // 실제로는 EntityManager에서 SanctuaryEntity를 조회하여 IdentityComponent.getLevel() 사용
        return 50; // 임시 기본값
    }

    /**
     * 보강(Fortify) 효과를 적용합니다.
     * 보강량 >= 현재 체력일 때 추가 15% 감소
     * 
     * @param damage        피해량
     * @param currentHp     현재 체력
     * @param fortifyAmount 보강량
     * @return 감소된 피해량
     */
    public double applyFortify(double damage, double currentHp, double fortifyAmount) {
        if (fortifyAmount >= currentHp) {
            return damage * 0.85; // 15% 감소
        }
        return damage;
    }

    /**
     * 보호막(Barrier)을 먼저 소모합니다.
     * 
     * @param damage        피해량
     * @param barrierAmount 현재 보호막량 (배열로 전달하여 수정)
     * @return 보호막이 흡수하지 못한 남은 피해량
     */
    public double absorbWithBarrier(double damage, double[] barrierAmount) {
        if (barrierAmount[0] <= 0)
            return damage;

        double absorbed = Math.min(barrierAmount[0], damage);
        barrierAmount[0] -= absorbed;

        return damage - absorbed;
    }
}
