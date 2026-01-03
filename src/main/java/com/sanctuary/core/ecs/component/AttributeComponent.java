package com.sanctuary.core.ecs.component;

import com.sanctuary.core.ecs.Component;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * 엔티티의 모든 스탯을 관리하는 속성 컨테이너 컴포넌트입니다.
 * 디아블로 IV의 데미지 버킷 시스템을 지원합니다.
 * 
 * 주요 스탯 키:
 * - Core: STRENGTH, INTELLIGENCE, WILLPOWER, DEXTERITY
 * - Offensive: WEAPON_DAMAGE, CRIT_CHANCE, ATTACK_SPEED, OVERPOWER_DAMAGE
 * - Defensive: ARMOR, RESISTANCE_FIRE, MAX_HP, BARRIER_MAX
 * - Utility: RESOURCE_MAX, RESOURCE_REGEN, COOLDOWN_REDUCTION
 */
public class AttributeComponent implements Component {

    private final Map<String, StatValue> attributes = new HashMap<>();
    private boolean dirty = true; // 재계산 필요 여부

    /**
     * 지정된 키의 최종 스탯 값을 반환합니다.
     * 
     * @param key 스탯 키
     * @return 최종 계산된 값 (존재하지 않으면 0)
     */
    public double getValue(String key) {
        StatValue stat = attributes.get(key);
        return stat != null ? stat.getFinalValue() : 0.0;
    }

    /**
     * 지정된 키의 StatValue 객체를 반환합니다.
     * 존재하지 않으면 새로 생성합니다.
     * 
     * @param key 스탯 키
     * @return StatValue 객체
     */
    public StatValue getOrCreate(String key) {
        return attributes.computeIfAbsent(key, k -> new StatValue());
    }

    /**
     * 지정된 키의 StatValue 객체를 반환합니다.
     * 
     * @param key 스탯 키
     * @return StatValue 또는 null
     */
    public StatValue get(String key) {
        return attributes.get(key);
    }

    /**
     * 스탯에 수정자를 추가합니다.
     * 
     * @param key   스탯 키
     * @param value 수정값
     * @param type  수정자 타입
     */
    public void addModifier(String key, double value, ModifierType type) {
        StatValue stat = getOrCreate(key);
        switch (type) {
            case BASE:
                stat.addBase(value);
                break;
            case ADDITIVE:
                stat.addAdditive(value);
                break;
            case MULTIPLICATIVE:
                stat.multiplyMultiplicative(value);
                break;
        }
        dirty = true;
    }

    /**
     * 스탯의 기본값을 설정합니다.
     * 
     * @param key  스탯 키
     * @param base 기본값
     */
    public void setBase(String key, double base) {
        getOrCreate(key).setBase(base);
        dirty = true;
    }

    /**
     * 스탯이 존재하는지 확인합니다.
     * 
     * @param key 스탯 키
     * @return 존재 여부
     */
    public boolean has(String key) {
        return attributes.containsKey(key);
    }

    /**
     * 모든 스탯 키를 반환합니다.
     * 
     * @return 스탯 키 Set
     */
    public Set<String> getKeys() {
        return attributes.keySet();
    }

    /**
     * 모든 스탯의 수정자를 초기화합니다.
     * 기본값은 유지됩니다.
     */
    public void resetAllModifiers() {
        for (StatValue stat : attributes.values()) {
            stat.resetModifiers();
        }
        dirty = true;
    }

    /**
     * 모든 스탯을 제거합니다.
     */
    public void clear() {
        attributes.clear();
        dirty = true;
    }

    /**
     * 재계산이 필요한지 여부를 반환합니다.
     * 
     * @return dirty 플래그
     */
    public boolean isDirty() {
        return dirty;
    }

    /**
     * dirty 플래그를 설정합니다.
     * 
     * @param dirty 새 값
     */
    public void setDirty(boolean dirty) {
        this.dirty = dirty;
    }

    /**
     * 다른 AttributeComponent의 값을 병합합니다.
     * 
     * @param other 병합할 컴포넌트
     */
    public void merge(AttributeComponent other) {
        for (Map.Entry<String, StatValue> entry : other.attributes.entrySet()) {
            getOrCreate(entry.getKey()).merge(entry.getValue());
        }
        dirty = true;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder("AttributeComponent{\n");
        for (Map.Entry<String, StatValue> entry : attributes.entrySet()) {
            sb.append("  ").append(entry.getKey()).append(": ")
                    .append(entry.getValue()).append("\n");
        }
        sb.append("}");
        return sb.toString();
    }
}
