package com.sanctuary.combat.skill;

import java.util.ArrayList;
import java.util.List;

/**
 * 스킬 데이터 모델
 * skills.json에서 로드되는 스킬 정의입니다.
 */
public class SkillData {

    // 기본 정보
    private String id;
    private String name;
    private String className;
    private String category;
    private String description;

    // 피해 계산
    private String damageType = "PHYSICAL";
    private double baseDamage = 1.0;
    private String damageModifier = "WEAPON_DAMAGE";

    // 자원
    private double resourceCost = 0;
    private double resourceGenerate = 0;
    private double cooldown = 0;
    private boolean isChanneled = false;

    // 스킬 트리 정보
    private int tier = 1; // 계층 (1-6)
    private int pointsRequired = 0; // 전제 스킬 포인트
    private List<String> prerequisites = new ArrayList<>(); // 선행 스킬 ID
    private int maxRank = 5; // 최대 랭크
    private double rankBonus = 0.10; // 랭크당 피해 증가

    // 효과
    private double luckyHitChance = 0.0;
    private double radius = 0.0;
    private boolean isRanged = false;
    private int tickRate = 0;

    // ===== Getters & Setters =====

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getClassName() {
        return className;
    }

    public void setClassName(String className) {
        this.className = className;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getDamageType() {
        return damageType;
    }

    public void setDamageType(String damageType) {
        this.damageType = damageType;
    }

    public double getBaseDamage() {
        return baseDamage;
    }

    public void setBaseDamage(double baseDamage) {
        this.baseDamage = baseDamage;
    }

    public String getDamageModifier() {
        return damageModifier;
    }

    public void setDamageModifier(String damageModifier) {
        this.damageModifier = damageModifier;
    }

    public double getResourceCost() {
        return resourceCost;
    }

    public void setResourceCost(double resourceCost) {
        this.resourceCost = resourceCost;
    }

    public double getResourceGenerate() {
        return resourceGenerate;
    }

    public void setResourceGenerate(double resourceGenerate) {
        this.resourceGenerate = resourceGenerate;
    }

    public double getCooldown() {
        return cooldown;
    }

    public void setCooldown(double cooldown) {
        this.cooldown = cooldown;
    }

    public boolean isChanneled() {
        return isChanneled;
    }

    public void setChanneled(boolean channeled) {
        isChanneled = channeled;
    }

    public int getTier() {
        return tier;
    }

    public void setTier(int tier) {
        this.tier = tier;
    }

    public int getPointsRequired() {
        return pointsRequired;
    }

    public void setPointsRequired(int pointsRequired) {
        this.pointsRequired = pointsRequired;
    }

    public List<String> getPrerequisites() {
        return prerequisites;
    }

    public void setPrerequisites(List<String> prerequisites) {
        this.prerequisites = prerequisites;
    }

    public int getMaxRank() {
        return maxRank;
    }

    public void setMaxRank(int maxRank) {
        this.maxRank = maxRank;
    }

    public double getRankBonus() {
        return rankBonus;
    }

    public void setRankBonus(double rankBonus) {
        this.rankBonus = rankBonus;
    }

    public double getLuckyHitChance() {
        return luckyHitChance;
    }

    public void setLuckyHitChance(double luckyHitChance) {
        this.luckyHitChance = luckyHitChance;
    }

    public double getRadius() {
        return radius;
    }

    public void setRadius(double radius) {
        this.radius = radius;
    }

    public boolean isRanged() {
        return isRanged;
    }

    public void setRanged(boolean ranged) {
        isRanged = ranged;
    }

    public int getTickRate() {
        return tickRate;
    }

    public void setTickRate(int tickRate) {
        this.tickRate = tickRate;
    }

    /**
     * 특정 랭크에서의 피해 배율을 계산합니다.
     */
    public double getDamageAtRank(int rank) {
        return baseDamage * (1.0 + (rank - 1) * rankBonus);
    }

    /**
     * 카테고리가 기본 스킬인지 확인합니다.
     */
    public boolean isBasic() {
        return "BASIC".equalsIgnoreCase(category);
    }

    /**
     * 카테고리가 핵심 스킬인지 확인합니다.
     */
    public boolean isCore() {
        return "CORE".equalsIgnoreCase(category);
    }

    /**
     * 카테고리가 궁극기인지 확인합니다.
     */
    public boolean isUltimate() {
        return "ULTIMATE".equalsIgnoreCase(category);
    }
}
