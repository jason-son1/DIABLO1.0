package com.sanctuary.items.aspect;

import java.util.Map;
import java.util.HashMap;

/**
 * 추출된 위상(Aspect) 인스턴스입니다.
 * 전설 아이템 분해 시 추출되어 플레이어의 위상 저장소에 보관됩니다.
 */
public class AspectInstance {

    private final String aspectId; // aspects.json의 ID
    private final String aspectName; // 표시 이름
    private final String category; // OFFENSIVE, DEFENSIVE, UTILITY, RESOURCE
    private final String description; // 효과 설명

    // 롤링된 실제 수치 (min~max 범위 내)
    private final Map<String, Double> rolledValues;

    // 추출 원본 정보
    private String sourceItemId; // 추출 원본 아이템 ID
    private long extractedTime; // 추출 시각

    // 적용 제한
    private final String[] allowedSlots; // 각인 가능한 슬롯
    private final String classRestriction; // 직업 제한 (null이면 모든 직업)

    public AspectInstance(String aspectId, String aspectName, String category,
            String description, String[] allowedSlots, String classRestriction) {
        this.aspectId = aspectId;
        this.aspectName = aspectName;
        this.category = category;
        this.description = description;
        this.allowedSlots = allowedSlots;
        this.classRestriction = classRestriction;
        this.rolledValues = new HashMap<>();
        this.extractedTime = System.currentTimeMillis();
    }

    // Fluent builder pattern for rolled values
    public AspectInstance withValue(String key, double value) {
        this.rolledValues.put(key, value);
        return this;
    }

    public AspectInstance withSourceItem(String sourceItemId) {
        this.sourceItemId = sourceItemId;
        return this;
    }

    // ===== Getters =====

    public String getAspectId() {
        return aspectId;
    }

    public String getAspectName() {
        return aspectName;
    }

    public String getCategory() {
        return category;
    }

    public String getDescription() {
        return description;
    }

    public Map<String, Double> getRolledValues() {
        return rolledValues;
    }

    public double getValue(String key) {
        return rolledValues.getOrDefault(key, 0.0);
    }

    public String getSourceItemId() {
        return sourceItemId;
    }

    public long getExtractedTime() {
        return extractedTime;
    }

    public String[] getAllowedSlots() {
        return allowedSlots;
    }

    public String getClassRestriction() {
        return classRestriction;
    }

    /**
     * 해당 슬롯에 각인이 가능한지 확인합니다.
     */
    public boolean canImprintToSlot(String slotType) {
        if (allowedSlots == null || allowedSlots.length == 0) {
            return true; // 제한 없음
        }
        for (String slot : allowedSlots) {
            if (slot.equalsIgnoreCase(slotType)) {
                return true;
            }
        }
        return false;
    }

    /**
     * 해당 직업이 사용 가능한지 확인합니다.
     */
    public boolean canUseByClass(String playerClass) {
        if (classRestriction == null || classRestriction.isEmpty()) {
            return true; // 모든 직업 사용 가능
        }
        return classRestriction.equalsIgnoreCase(playerClass);
    }

    /**
     * 설명 텍스트를 생성합니다 (실제 수치 포함).
     */
    public String getFormattedDescription() {
        String result = description;
        for (Map.Entry<String, Double> entry : rolledValues.entrySet()) {
            String placeholder = "{" + entry.getKey() + "}";
            String value = String.format("%.1f", entry.getValue() * 100) + "%";
            result = result.replace(placeholder, value);
        }
        return result;
    }

    @Override
    public String toString() {
        return String.format("AspectInstance{id=%s, name=%s, category=%s, values=%s}",
                aspectId, aspectName, category, rolledValues);
    }
}
