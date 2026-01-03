package com.sanctuary.core.model;

import java.util.Map;

/**
 * 아이템의 기본 데이터(Base)를 담는 DTO 클래스입니다.
 * 재질, 기본 공격력/방어력 등을 정의합니다.
 */
public class ItemBaseData {
    private String id; // 예: ancestral_sword
    private String material; // Minecraft Material Name (e.g., DIAMOND_SWORD)
    private String itemType; // 예: ONE_HANDED_SWORD
    private Map<String, Double> baseStats; // 기본 스탯 (공격력 등)

    public ItemBaseData() {
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getMaterial() {
        return material;
    }

    public void setMaterial(String material) {
        this.material = material;
    }

    public String getItemType() {
        return itemType;
    }

    public void setItemType(String itemType) {
        this.itemType = itemType;
    }

    public Map<String, Double> getBaseStats() {
        return baseStats;
    }

    public void setBaseStats(Map<String, Double> baseStats) {
        this.baseStats = baseStats;
    }
}
