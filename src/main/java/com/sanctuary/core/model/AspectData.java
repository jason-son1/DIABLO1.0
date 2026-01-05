package com.sanctuary.core.model;

import java.util.Map;

/**
 * Aspect(위상) 데이터 정의입니다.
 * aspects.json에서 로드됩니다.
 */
public class AspectData {

    private String id;
    private String name;
    private String category; // OFFENSIVE, DEFENSIVE, UTILITY, RESOURCE
    private String description;
    private String[] allowedSlots; // 각인 가능 슬롯
    private String classRestriction; // 직업 제한
    private Map<String, Object> effect; // 효과 정의
    private String source; // 획득처

    public AspectData() {
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

    public String[] getAllowedSlots() {
        return allowedSlots;
    }

    public void setAllowedSlots(String[] allowedSlots) {
        this.allowedSlots = allowedSlots;
    }

    public String getClassRestriction() {
        return classRestriction;
    }

    public void setClassRestriction(String classRestriction) {
        this.classRestriction = classRestriction;
    }

    public Map<String, Object> getEffect() {
        return effect;
    }

    public void setEffect(Map<String, Object> effect) {
        this.effect = effect;
    }

    public String getSource() {
        return source;
    }

    public void setSource(String source) {
        this.source = source;
    }
}
