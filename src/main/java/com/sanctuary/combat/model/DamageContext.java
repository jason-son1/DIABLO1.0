package com.sanctuary.combat.model;

import com.sanctuary.combat.stat.AttributeContainer;
import org.bukkit.entity.LivingEntity;

import java.util.HashSet;
import java.util.Set;

/**
 * DamageContext
 * 데미지 계산에 필요한 모든 문맥 정보를 담는 POJO 객체입니다.
 */
public class DamageContext {

    private final LivingEntity attacker;
    private final LivingEntity victim;

    // 스냅샷된 스탯
    private final AttributeContainer attackerStats;
    private final AttributeContainer victimStats;

    // 스킬 계수 (Skill Coefficient)
    private double skillCoefficient = 1.0; // 기본 평타 = 100%

    // 결과 플래그
    private boolean isCritical = false;
    private boolean isOverpower = false;
    private boolean isVulnerable = false; // 타겟이 취약 상태인가?

    // 태그 (예: "Physical", "Fire", "Melee", "CoreSkill")
    private final Set<String> tags = new HashSet<>();

    public DamageContext(LivingEntity attacker, LivingEntity victim, AttributeContainer attackerStats,
            AttributeContainer victimStats) {
        this.attacker = attacker;
        this.victim = victim;
        this.attackerStats = attackerStats;
        this.victimStats = victimStats;
    }

    // Getters & Setters
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

    public Set<String> getTags() {
        return tags;
    }

    public void addTag(String tag) {
        this.tags.add(tag);
    }

    public boolean hasTag(String tag) {
        return this.tags.contains(tag);
    }
}
