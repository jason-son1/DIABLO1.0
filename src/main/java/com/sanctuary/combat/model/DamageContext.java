package com.sanctuary.combat.model;

import com.sanctuary.combat.stat.AttributeContainer;
import org.bukkit.entity.LivingEntity;

import java.util.EnumMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * DamageContext
 * 데미지 계산에 필요한 모든 문맥 정보를 담는 POJO 객체입니다.
 * 
 * 디아블로 4 시즌 6 공식:
 * FinalDamage = WeaponDamage * SkillCoeff * (1 + AdditiveBuckets) *
 * MultiplierBuckets
 * * CritMultiplier * VulnerableMultiplier + OverpowerDamage
 */
public class DamageContext {

    // ===== 피해 버킷 정의 =====
    public enum DamageBucket {
        // 가산 버킷 (Additive)
        SKILL_DAMAGE, // 스킬 피해 증가
        PHYSICAL_DAMAGE, // 물리 피해 증가
        FIRE_DAMAGE, // 화염 피해 증가
        COLD_DAMAGE, // 냉기 피해 증가
        LIGHTNING_DAMAGE, // 번개 피해 증가
        POISON_DAMAGE, // 독 피해 증가
        SHADOW_DAMAGE, // 암흑 피해 증가
        CORE_SKILL_DAMAGE, // 핵심 스킬 피해 증가
        BASIC_SKILL_DAMAGE, // 기본 스킬 피해 증가

        // 곱셈 버킷 (Multiplicative)
        CLOSE_DAMAGE, // 근접 피해 증가 (x)
        DISTANT_DAMAGE, // 원거리 피해 증가 (x)
        DAMAGE_VS_INJURED, // 부상 적 피해 증가 (x)
        DAMAGE_VS_HEALTHY, // 건강 적 피해 증가 (x)
        DAMAGE_VS_CC, // CC된 적 피해 증가 (x)
        DAMAGE_VS_ELITES, // 정예 피해 증가 (x)
        DAMAGE_VS_BURNING, // 불타는 적 피해 증가 (x)
        DAMAGE_VS_BLEEDING, // 출혈 적 피해 증가 (x)
        DAMAGE_VS_FROZEN, // 동결 적 피해 증가 (x)
        DAMAGE_WHILE_BERSERKING, // 광폭 중 피해 증가 (x)
        DAMAGE_WHILE_HEALTHY, // 건강할 때 피해 증가 (x)

        // 특수 버킷
        VULNERABLE_DAMAGE, // 취약 피해 증가
        OVERPOWER_DAMAGE, // 제압 피해 증가
        CRITICAL_DAMAGE // 치명타 피해 증가
    }

    private final LivingEntity attacker;
    private final LivingEntity victim;

    // 스냅샷된 스탯
    private final AttributeContainer attackerStats;
    private final AttributeContainer victimStats;

    // 스킬 계수 (Skill Coefficient)
    private double skillCoefficient = 1.0;

    // 결과 플래그
    private boolean isCritical = false;
    private boolean isOverpower = false;
    private boolean isVulnerable = false;
    private boolean isLuckyHit = false;

    // 확률
    private double critChance = 0.05; // 기본 5%
    private double overpowerChance = 0.03; // 기본 3%
    private double luckyHitChance = 0.0; // 스킬별 설정

    // 배율
    private double critMultiplier = 1.5; // 기본 150%
    private double vulnerableMultiplier = 1.2; // 기본 120%
    private double overpowerBonus = 0.0; // 추가 제압 피해

    // 피해 버킷 (가산)
    private final Map<DamageBucket, Double> additiveBuckets = new EnumMap<>(DamageBucket.class);
    // 피해 버킷 (곱셈)
    private final Map<DamageBucket, Double> multiplicativeBuckets = new EnumMap<>(DamageBucket.class);

    // 최종 계산 결과
    private double baseDamage = 0;
    private double finalDamage = 0;
    private double overpowerDamage = 0;

    // 태그 (예: "Physical", "Fire", "Melee", "CoreSkill")
    private final Set<String> tags = new HashSet<>();

    // 피해 유형
    private String damageType = "PHYSICAL";

    public DamageContext(LivingEntity attacker, LivingEntity victim, AttributeContainer attackerStats,
            AttributeContainer victimStats) {
        this.attacker = attacker;
        this.victim = victim;
        this.attackerStats = attackerStats;
        this.victimStats = victimStats;

        // 곱셈 버킷 기본값 1.0
        for (DamageBucket bucket : DamageBucket.values()) {
            if (bucket.ordinal() >= DamageBucket.CLOSE_DAMAGE.ordinal()) {
                multiplicativeBuckets.put(bucket, 1.0);
            }
        }
    }

    // ===== 피해 버킷 API =====

    /**
     * 가산 피해 버킷에 값을 추가합니다.
     */
    public void addAdditiveBucket(DamageBucket bucket, double value) {
        additiveBuckets.merge(bucket, value, Double::sum);
    }

    /**
     * 곱셈 피해 버킷에 배율을 적용합니다.
     */
    public void addMultiplicativeBucket(DamageBucket bucket, double multiplier) {
        multiplicativeBuckets.merge(bucket, multiplier, (a, b) -> a * b);
    }

    /**
     * 가산 버킷의 총합을 계산합니다.
     */
    public double getTotalAdditive() {
        return additiveBuckets.values().stream().mapToDouble(Double::doubleValue).sum();
    }

    /**
     * 곱셈 버킷의 총 배율을 계산합니다.
     */
    public double getTotalMultiplicative() {
        return multiplicativeBuckets.values().stream()
                .reduce(1.0, (a, b) -> a * b);
    }

    /**
     * 치명타 판정을 수행합니다.
     */
    public boolean rollCritical() {
        if (Math.random() < critChance) {
            this.isCritical = true;
            return true;
        }
        return false;
    }

    /**
     * 제압 판정을 수행합니다.
     */
    public boolean rollOverpower() {
        if (Math.random() < overpowerChance) {
            this.isOverpower = true;
            return true;
        }
        return false;
    }

    /**
     * 행운의 적중 판정을 수행합니다.
     */
    public boolean rollLuckyHit() {
        if (luckyHitChance > 0 && Math.random() < luckyHitChance) {
            this.isLuckyHit = true;
            return true;
        }
        return false;
    }

    // ===== Getters & Setters =====

    public LivingEntity getAttacker() {
        return attacker;
    }

    public LivingEntity getVictim() {
        return victim;
    }

    public AttributeContainer getAttackerStats() {
        return attackerStats;
    }

    public AttributeContainer getVictimStats() {
        return victimStats;
    }

    public double getSkillCoefficient() {
        return skillCoefficient;
    }

    public void setSkillCoefficient(double skillCoefficient) {
        this.skillCoefficient = skillCoefficient;
    }

    public boolean isCritical() {
        return isCritical;
    }

    public void setCritical(boolean critical) {
        isCritical = critical;
    }

    public boolean isOverpower() {
        return isOverpower;
    }

    public void setOverpower(boolean overpower) {
        isOverpower = overpower;
    }

    public boolean isVulnerable() {
        return isVulnerable;
    }

    public void setVulnerable(boolean vulnerable) {
        isVulnerable = vulnerable;
    }

    public boolean isLuckyHit() {
        return isLuckyHit;
    }

    public void setLuckyHit(boolean luckyHit) {
        isLuckyHit = luckyHit;
    }

    public double getCritChance() {
        return critChance;
    }

    public void setCritChance(double critChance) {
        this.critChance = critChance;
    }

    public double getOverpowerChance() {
        return overpowerChance;
    }

    public void setOverpowerChance(double overpowerChance) {
        this.overpowerChance = overpowerChance;
    }

    public double getLuckyHitChance() {
        return luckyHitChance;
    }

    public void setLuckyHitChance(double luckyHitChance) {
        this.luckyHitChance = luckyHitChance;
    }

    public double getCritMultiplier() {
        return critMultiplier;
    }

    public void setCritMultiplier(double critMultiplier) {
        this.critMultiplier = critMultiplier;
    }

    public double getVulnerableMultiplier() {
        return vulnerableMultiplier;
    }

    public void setVulnerableMultiplier(double vulnerableMultiplier) {
        this.vulnerableMultiplier = vulnerableMultiplier;
    }

    public double getOverpowerBonus() {
        return overpowerBonus;
    }

    public void setOverpowerBonus(double overpowerBonus) {
        this.overpowerBonus = overpowerBonus;
    }

    public double getBaseDamage() {
        return baseDamage;
    }

    public void setBaseDamage(double baseDamage) {
        this.baseDamage = baseDamage;
    }

    public double getFinalDamage() {
        return finalDamage;
    }

    public void setFinalDamage(double finalDamage) {
        this.finalDamage = finalDamage;
    }

    public double getOverpowerDamage() {
        return overpowerDamage;
    }

    public void setOverpowerDamage(double overpowerDamage) {
        this.overpowerDamage = overpowerDamage;
    }

    public String getDamageType() {
        return damageType;
    }

    public void setDamageType(String damageType) {
        this.damageType = damageType;
    }

    public Set<String> getTags() {
        return tags;
    }

    public void addTag(String tag) {
        this.tags.add(tag);
    }

    public boolean hasTag(String tag) {
        return this.tags.contains(tag);
    }

    public Map<DamageBucket, Double> getAdditiveBuckets() {
        return additiveBuckets;
    }

    public Map<DamageBucket, Double> getMultiplicativeBuckets() {
        return multiplicativeBuckets;
    }
}
