package com.sanctuary.core.ecs.component;

import com.sanctuary.core.ecs.Component;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

/**
 * 엔티티의 태그들을 관리하는 컴포넌트입니다.
 * 태그는 엔티티의 특성이나 상태를 나타내는 문자열입니다.
 * 
 * 일반적인 태그 예시:
 * - 카테고리: ELITE, BOSS, MINION
 * - 종족: UNDEAD, DEMON, BEAST, HUMAN
 * - 상태: VULNERABLE(취약), FORTIFIED(보강), STUNNED, BURNING
 * - 스킬 타입: BASIC_SKILL, CORE_SKILL, ULTIMATE
 */
public class TagComponent implements Component {

    private final Set<String> tags = new HashSet<>();

    /**
     * 태그를 추가합니다.
     * 
     * @param tag 추가할 태그
     * @return 이 컴포넌트 (체이닝용)
     */
    public TagComponent add(String tag) {
        if (tag != null && !tag.isBlank()) {
            tags.add(tag.toUpperCase());
        }
        return this;
    }

    /**
     * 여러 태그를 추가합니다.
     * 
     * @param tags 추가할 태그들
     * @return 이 컴포넌트 (체이닝용)
     */
    public TagComponent addAll(String... tags) {
        for (String tag : tags) {
            add(tag);
        }
        return this;
    }

    /**
     * 태그를 제거합니다.
     * 
     * @param tag 제거할 태그
     * @return 제거 성공 여부
     */
    public boolean remove(String tag) {
        return tags.remove(tag.toUpperCase());
    }

    /**
     * 태그가 존재하는지 확인합니다.
     * 
     * @param tag 확인할 태그
     * @return 존재 여부
     */
    public boolean has(String tag) {
        return tags.contains(tag.toUpperCase());
    }

    /**
     * 주어진 모든 태그가 존재하는지 확인합니다.
     * 
     * @param requiredTags 확인할 태그들
     * @return 모두 존재하면 true
     */
    public boolean hasAll(String... requiredTags) {
        for (String tag : requiredTags) {
            if (!has(tag))
                return false;
        }
        return true;
    }

    /**
     * 주어진 태그 중 하나라도 존재하는지 확인합니다.
     * 
     * @param anyTags 확인할 태그들
     * @return 하나라도 존재하면 true
     */
    public boolean hasAny(String... anyTags) {
        for (String tag : anyTags) {
            if (has(tag))
                return true;
        }
        return false;
    }

    /**
     * 모든 태그를 반환합니다 (읽기 전용).
     * 
     * @return 태그 Set
     */
    public Set<String> getAll() {
        return Collections.unmodifiableSet(tags);
    }

    /**
     * 모든 태그를 제거합니다.
     */
    public void clear() {
        tags.clear();
    }

    /**
     * 태그 개수를 반환합니다.
     * 
     * @return 태그 개수
     */
    public int size() {
        return tags.size();
    }

    /**
     * 태그가 비어있는지 확인합니다.
     * 
     * @return 비어있으면 true
     */
    public boolean isEmpty() {
        return tags.isEmpty();
    }

    @Override
    public String toString() {
        return "TagComponent{" + String.join(", ", tags) + "}";
    }
}
