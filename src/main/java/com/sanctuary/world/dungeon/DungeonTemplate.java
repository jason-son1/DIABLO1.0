package com.sanctuary.world.dungeon;

/**
 * 던전 템플릿
 * dungeons.json에서 로드되는 던전 정의입니다.
 */
public class DungeonTemplate {

    private String id;
    private String name;
    private String description;

    // 월드 설정
    private String templateWorld; // 복사할 템플릿 월드 이름
    private String spawnLocation; // 스폰 좌표 (x,y,z)

    // 난이도
    private int minTier = 1;
    private int maxTier = 100;
    private int baseLevelRequirement = 1;

    // 목표
    private String[] objectives; // ["kill_boss", "clear_enemies", "survive"]
    private int timeLimit = 0; // 제한 시간 (초), 0 = 무제한

    // 몬스터
    private String[] monsterIds; // 스폰될 몬스터 ID 목록
    private String bossId; // 보스 몬스터 ID

    // 보상
    private double goldMultiplier = 1.0;
    private double xpMultiplier = 1.0;
    private double lootMultiplier = 1.0;
    private int guaranteedRares = 0;

    // 시그니처 (악몽 던전용)
    private boolean requiresSignet = false;
    private String signetId;

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

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getTemplateWorld() {
        return templateWorld;
    }

    public void setTemplateWorld(String templateWorld) {
        this.templateWorld = templateWorld;
    }

    public String getSpawnLocation() {
        return spawnLocation;
    }

    public void setSpawnLocation(String spawnLocation) {
        this.spawnLocation = spawnLocation;
    }

    public int getMinTier() {
        return minTier;
    }

    public void setMinTier(int minTier) {
        this.minTier = minTier;
    }

    public int getMaxTier() {
        return maxTier;
    }

    public void setMaxTier(int maxTier) {
        this.maxTier = maxTier;
    }

    public int getBaseLevelRequirement() {
        return baseLevelRequirement;
    }

    public void setBaseLevelRequirement(int baseLevelRequirement) {
        this.baseLevelRequirement = baseLevelRequirement;
    }

    public String[] getObjectives() {
        return objectives;
    }

    public void setObjectives(String[] objectives) {
        this.objectives = objectives;
    }

    public int getTimeLimit() {
        return timeLimit;
    }

    public void setTimeLimit(int timeLimit) {
        this.timeLimit = timeLimit;
    }

    public String[] getMonsterIds() {
        return monsterIds;
    }

    public void setMonsterIds(String[] monsterIds) {
        this.monsterIds = monsterIds;
    }

    public String getBossId() {
        return bossId;
    }

    public void setBossId(String bossId) {
        this.bossId = bossId;
    }

    public double getGoldMultiplier() {
        return goldMultiplier;
    }

    public void setGoldMultiplier(double goldMultiplier) {
        this.goldMultiplier = goldMultiplier;
    }

    public double getXpMultiplier() {
        return xpMultiplier;
    }

    public void setXpMultiplier(double xpMultiplier) {
        this.xpMultiplier = xpMultiplier;
    }

    public double getLootMultiplier() {
        return lootMultiplier;
    }

    public void setLootMultiplier(double lootMultiplier) {
        this.lootMultiplier = lootMultiplier;
    }

    public int getGuaranteedRares() {
        return guaranteedRares;
    }

    public void setGuaranteedRares(int guaranteedRares) {
        this.guaranteedRares = guaranteedRares;
    }

    public boolean isRequiresSignet() {
        return requiresSignet;
    }

    public void setRequiresSignet(boolean requiresSignet) {
        this.requiresSignet = requiresSignet;
    }

    public String getSignetId() {
        return signetId;
    }

    public void setSignetId(String signetId) {
        this.signetId = signetId;
    }

    /**
     * 티어에 따른 몬스터 레벨 보정을 계산합니다.
     */
    public int calculateMonsterLevel(int tier, int baseLevel) {
        // 티어당 +1 레벨, 최소 baseLevelRequirement
        return Math.max(baseLevelRequirement, baseLevel + tier);
    }

    /**
     * 티어에 따른 보상 배율을 계산합니다.
     */
    public double calculateRewardMultiplier(int tier) {
        // 티어 10마다 10% 추가 보상
        return 1.0 + (tier / 10.0 * 0.1);
    }
}
