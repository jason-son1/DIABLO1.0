package com.sanctuary.combat.event;

import com.sanctuary.core.ecs.SanctuaryEntity;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * 전투 컨텍스트
 * 전투 상황에서 공격자, 피격자, 스킬, 태그 등 모든 관련 정보를 담는 컨테이너입니다.
 * 
 * 사용 예시:
 * 
 * <pre>
 * CombatContext ctx = CombatContext.builder()
 *         .attacker(attackerEntity)
 *         .victim(victimEntity)
 *         .skillId("fireball")
 *         .addTag("FIRE")
 *         .build();
 * </pre>
 */
public class CombatContext {

    // ===== 기본 정보 =====
    private final UUID contextId;
    private final long timestamp;

    // ===== 참여자 =====
    private SanctuaryEntity attacker;
    private SanctuaryEntity victim;

    // ===== 스킬/아이템 정보 =====
    private String skillId;
    private double skillCoefficient = 1.0;
    private String itemId;

    // ===== 태그 =====
    private final Set<String> tags = new HashSet<>();

    // ===== 거리/위치 =====
    private double distance;

    // ===== 커스텀 데이터 =====
    private final Map<String, Object> customData = new HashMap<>();

    public CombatContext() {
        this.contextId = UUID.randomUUID();
        this.timestamp = System.currentTimeMillis();
    }

    // ===== Fluent Setters =====

    public CombatContext attacker(SanctuaryEntity attacker) {
        this.attacker = attacker;
        return this;
    }

    public CombatContext victim(SanctuaryEntity victim) {
        this.victim = victim;
        return this;
    }

    public CombatContext skillId(String skillId) {
        this.skillId = skillId;
        return this;
    }

    public CombatContext skillCoefficient(double coefficient) {
        this.skillCoefficient = coefficient;
        return this;
    }

    public CombatContext itemId(String itemId) {
        this.itemId = itemId;
        return this;
    }

    public CombatContext distance(double distance) {
        this.distance = distance;
        return this;
    }

    public CombatContext addTag(String tag) {
        this.tags.add(tag.toUpperCase());
        return this;
    }

    public CombatContext addTags(String... tags) {
        for (String tag : tags) {
            this.tags.add(tag.toUpperCase());
        }
        return this;
    }

    public CombatContext put(String key, Object value) {
        this.customData.put(key, value);
        return this;
    }

    // ===== Getters =====

    public UUID getContextId() {
        return contextId;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public SanctuaryEntity getAttacker() {
        return attacker;
    }

    public SanctuaryEntity getVictim() {
        return victim;
    }

    public String getSkillId() {
        return skillId;
    }

    public double getSkillCoefficient() {
        return skillCoefficient;
    }

    public String getItemId() {
        return itemId;
    }

    public double getDistance() {
        return distance;
    }

    public boolean hasTag(String tag) {
        return tags.contains(tag.toUpperCase());
    }

    public boolean hasAnyTag(String... checkTags) {
        for (String tag : checkTags) {
            if (tags.contains(tag.toUpperCase())) {
                return true;
            }
        }
        return false;
    }

    public Set<String> getTags() {
        return new HashSet<>(tags);
    }

    @SuppressWarnings("unchecked")
    public <T> T get(String key) {
        return (T) customData.get(key);
    }

    public <T> T get(String key, T defaultValue) {
        Object value = customData.get(key);
        if (value == null)
            return defaultValue;
        return (T) value;
    }

    public Map<String, Object> getCustomData() {
        return new HashMap<>(customData);
    }

    // ===== Builder Pattern =====

    public static CombatContext builder() {
        return new CombatContext();
    }

    public CombatContext build() {
        return this;
    }

    // ===== Copy =====

    /**
     * 현재 컨텍스트의 복사본을 생성합니다.
     * 
     * @return 새로운 CombatContext 인스턴스
     */
    public CombatContext copy() {
        CombatContext copy = new CombatContext();
        copy.attacker = this.attacker;
        copy.victim = this.victim;
        copy.skillId = this.skillId;
        copy.skillCoefficient = this.skillCoefficient;
        copy.itemId = this.itemId;
        copy.distance = this.distance;
        copy.tags.addAll(this.tags);
        copy.customData.putAll(this.customData);
        return copy;
    }

    @Override
    public String toString() {
        return "CombatContext{" +
                "contextId=" + contextId +
                ", attacker=" + (attacker != null ? attacker.getUuid() : "null") +
                ", victim=" + (victim != null ? victim.getUuid() : "null") +
                ", skillId='" + skillId + '\'' +
                ", tags=" + tags +
                '}';
    }
}
