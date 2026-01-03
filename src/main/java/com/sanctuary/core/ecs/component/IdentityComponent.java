package com.sanctuary.core.ecs.component;

import com.sanctuary.core.ecs.Component;

/**
 * 엔티티의 정체성 정보를 저장하는 컴포넌트입니다.
 * 템플릿 ID, 레벨, 패밀리, 표시 이름 등을 관리합니다.
 */
public class IdentityComponent implements Component {

    private final String templateId; // 데이터 정의 ID (예: "fallen_shaman_01")
    private String displayName; // 표시 이름
    private int level; // 엔티티 레벨
    private String family; // 패밀리/종족 (예: "UNDEAD", "DEMON")
    private String category; // 카테고리 (예: "ELITE", "BOSS", "NORMAL")
    private String job; // 직업 (플레이어 전용, 예: "BARBARIAN", "SORCERER")

    /**
     * IdentityComponent를 생성합니다.
     * 
     * @param templateId 템플릿 ID
     */
    public IdentityComponent(String templateId) {
        this.templateId = templateId;
        this.displayName = templateId;
        this.level = 1;
        this.family = "UNKNOWN";
        this.category = "NORMAL";
    }

    /**
     * IdentityComponent를 생성합니다.
     * 
     * @param templateId 템플릿 ID
     * @param level      레벨
     */
    public IdentityComponent(String templateId, int level) {
        this(templateId);
        this.level = level;
    }

    // ===== Getters & Setters =====

    public String getTemplateId() {
        return templateId;
    }

    public String getDisplayName() {
        return displayName;
    }

    public IdentityComponent setDisplayName(String displayName) {
        this.displayName = displayName;
        return this;
    }

    public int getLevel() {
        return level;
    }

    public IdentityComponent setLevel(int level) {
        this.level = level;
        return this;
    }

    public String getFamily() {
        return family;
    }

    public IdentityComponent setFamily(String family) {
        this.family = family;
        return this;
    }

    public String getCategory() {
        return category;
    }

    public IdentityComponent setCategory(String category) {
        this.category = category;
        return this;
    }

    public String getJob() {
        return job;
    }

    public IdentityComponent setJob(String job) {
        this.job = job;
        return this;
    }

    /**
     * 이 엔티티가 보스인지 확인합니다.
     * 
     * @return 보스 여부
     */
    public boolean isBoss() {
        return "BOSS".equalsIgnoreCase(category);
    }

    /**
     * 이 엔티티가 정예(Elite)인지 확인합니다.
     * 
     * @return 정예 여부
     */
    public boolean isElite() {
        return "ELITE".equalsIgnoreCase(category);
    }

    @Override
    public String toString() {
        return String.format("IdentityComponent{id='%s', name='%s', lv=%d, family='%s', category='%s'}",
                templateId, displayName, level, family, category);
    }
}
