package com.sanctuary.combat.event;

/**
 * 피해 발생 이벤트
 * 엔티티가 다른 엔티티에게 피해를 입힐 때 발생합니다.
 */
public class DamageDealtEvent extends AbstractCombatEvent {

    // ===== 피해 정보 =====
    private double baseDamage;
    private double finalDamage;
    private String damageType = "PHYSICAL";

    // ===== 특수 상태 =====
    private boolean critical = false;
    private boolean overpower = false;
    private boolean vuln = false;
    private boolean luckyHit = false;

    // ===== 추가 정보 =====
    private double critMultiplier = 1.5;
    private double vulnMultiplier = 1.2;
    private double overpowerDamage = 0.0;

    public DamageDealtEvent(CombatContext context) {
        super(CombatEventType.DAMAGE_DEALT, context);
    }

    public DamageDealtEvent(CombatContext context, double baseDamage) {
        super(CombatEventType.DAMAGE_DEALT, context);
        this.baseDamage = baseDamage;
        this.finalDamage = baseDamage;
    }

    // ===== Fluent Setters =====

    public DamageDealtEvent baseDamage(double baseDamage) {
        this.baseDamage = baseDamage;
        return this;
    }

    public DamageDealtEvent finalDamage(double finalDamage) {
        this.finalDamage = finalDamage;
        return this;
    }

    public DamageDealtEvent damageType(String damageType) {
        this.damageType = damageType.toUpperCase();
        return this;
    }

    public DamageDealtEvent critical(boolean critical) {
        this.critical = critical;
        return this;
    }

    public DamageDealtEvent overpower(boolean overpower) {
        this.overpower = overpower;
        return this;
    }

    public DamageDealtEvent vulnerable(boolean vuln) {
        this.vuln = vuln;
        return this;
    }

    public DamageDealtEvent luckyHit(boolean luckyHit) {
        this.luckyHit = luckyHit;
        return this;
    }

    public DamageDealtEvent critMultiplier(double critMultiplier) {
        this.critMultiplier = critMultiplier;
        return this;
    }

    public DamageDealtEvent vulnMultiplier(double vulnMultiplier) {
        this.vulnMultiplier = vulnMultiplier;
        return this;
    }

    public DamageDealtEvent overpowerDamage(double overpowerDamage) {
        this.overpowerDamage = overpowerDamage;
        return this;
    }

    // ===== Getters =====

    public double getBaseDamage() {
        return baseDamage;
    }

    public double getFinalDamage() {
        return finalDamage;
    }

    public String getDamageType() {
        return damageType;
    }

    public boolean isCritical() {
        return critical;
    }

    public boolean isOverpower() {
        return overpower;
    }

    public boolean isVulnerable() {
        return vuln;
    }

    public boolean isLuckyHit() {
        return luckyHit;
    }

    public double getCritMultiplier() {
        return critMultiplier;
    }

    public double getVulnMultiplier() {
        return vulnMultiplier;
    }

    public double getOverpowerDamage() {
        return overpowerDamage;
    }

    // ===== Modifiers =====

    /**
     * 최종 피해를 수정합니다. (리스너에서 피해량 조정용)
     */
    public void setFinalDamage(double finalDamage) {
        this.finalDamage = finalDamage;
    }

    /**
     * 최종 피해에 배율을 적용합니다.
     */
    public void multiplyFinalDamage(double multiplier) {
        this.finalDamage *= multiplier;
    }

    /**
     * 최종 피해에 추가 피해를 더합니다.
     */
    public void addFinalDamage(double amount) {
        this.finalDamage += amount;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder("DamageDealtEvent{");
        sb.append("damage=").append(String.format("%.1f", finalDamage));
        sb.append(", type=").append(damageType);
        if (critical)
            sb.append(" [CRIT]");
        if (overpower)
            sb.append(" [OVERPOWER]");
        if (vuln)
            sb.append(" [VULN]");
        if (luckyHit)
            sb.append(" [LUCKY]");
        sb.append("}");
        return sb.toString();
    }
}
