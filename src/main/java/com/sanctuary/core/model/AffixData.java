package com.sanctuary.core.model;

import java.util.List;
import java.util.Map;

/**
 * 아이템 어픽스(Affix) 데이터를 담는 DTO 클래스입니다.
 * 접두사/접미사 및 부여되는 스탯 정보를 포함합니다.
 */
public class AffixData {
    private String id;
    private String name;
    private String category; // 카테고리 (weapon, attack, defense 등)
    private List<String> tags; // 태그 목록 (TEMPERING 등)
    private List<String> allowedItemTypes; // 적용 가능한 아이템 타입 목록
    private Map<String, Double> statModifiers; // 스탯 ID와 수치 (범위로 확장 가능)

    public AffixData() {
    }

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

    public List<String> getAllowedItemTypes() {
        return allowedItemTypes;
    }

    public void setAllowedItemTypes(List<String> allowedItemTypes) {
        this.allowedItemTypes = allowedItemTypes;
    }

    public Map<String, Double> getStatModifiers() {
        return statModifiers;
    }

    public void setStatModifiers(Map<String, Double> statModifiers) {
        this.statModifiers = statModifiers;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public List<String> getTags() {
        return tags;
    }

    public void setTags(List<String> tags) {
        this.tags = tags;
    }
}
