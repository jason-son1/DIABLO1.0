package com.sanctuary.combat.skill;

import com.sanctuary.core.ecs.Component;

import java.util.HashMap;
import java.util.Map;

/**
 * 플레이어 스킬 상태 Component
 * 투자된 스킬 포인트, 쿨타임 등을 관리합니다.
 */
public class SkillComponent implements Component {

    // 스킬별 투자 랭크
    private final Map<String, Integer> skillRanks = new HashMap<>();

    // 스킬별 쿨타임 (밀리초 기준)
    private final Map<String, Long> cooldowns = new HashMap<>();

    // 사용 가능한 스킬 포인트
    private int availablePoints = 0;

    // 총 투자된 포인트
    private int totalInvestedPoints = 0;

    /**
     * 스킬 랭크를 반환합니다.
     */
    public int getSkillRank(String skillId) {
        return skillRanks.getOrDefault(skillId, 0);
    }

    /**
     * 스킬에 포인트를 투자합니다.
     * 
     * @return 투자 성공 여부
     */
    public boolean investPoint(String skillId, int maxRank) {
        if (availablePoints <= 0) {
            return false;
        }

        int currentRank = skillRanks.getOrDefault(skillId, 0);
        if (currentRank >= maxRank) {
            return false;
        }

        skillRanks.put(skillId, currentRank + 1);
        availablePoints--;
        totalInvestedPoints++;
        return true;
    }

    /**
     * 스킬에서 포인트를 회수합니다.
     * 
     * @return 회수 성공 여부
     */
    public boolean refundPoint(String skillId) {
        int currentRank = skillRanks.getOrDefault(skillId, 0);
        if (currentRank <= 0) {
            return false;
        }

        skillRanks.put(skillId, currentRank - 1);
        if (currentRank - 1 == 0) {
            skillRanks.remove(skillId);
        }
        availablePoints++;
        totalInvestedPoints--;
        return true;
    }

    /**
     * 스킬 포인트를 추가합니다. (레벨업 시)
     */
    public void addPoints(int points) {
        availablePoints += points;
    }

    /**
     * 사용 가능한 포인트를 반환합니다.
     */
    public int getAvailablePoints() {
        return availablePoints;
    }

    /**
     * 총 투자된 포인트를 반환합니다.
     */
    public int getTotalInvestedPoints() {
        return totalInvestedPoints;
    }

    /**
     * 스킬이 쿨타임 중인지 확인합니다.
     */
    public boolean isOnCooldown(String skillId) {
        Long cooldownEnd = cooldowns.get(skillId);
        if (cooldownEnd == null) {
            return false;
        }
        return System.currentTimeMillis() < cooldownEnd;
    }

    /**
     * 스킬의 남은 쿨타임을 반환합니다. (초)
     */
    public double getRemainingCooldown(String skillId) {
        Long cooldownEnd = cooldowns.get(skillId);
        if (cooldownEnd == null) {
            return 0;
        }
        long remaining = cooldownEnd - System.currentTimeMillis();
        return remaining > 0 ? remaining / 1000.0 : 0;
    }

    /**
     * 스킬 쿨타임을 시작합니다.
     */
    public void startCooldown(String skillId, double seconds) {
        long cooldownEnd = System.currentTimeMillis() + (long) (seconds * 1000);
        cooldowns.put(skillId, cooldownEnd);
    }

    /**
     * 스킬 쿨타임을 초기화합니다.
     */
    public void resetCooldown(String skillId) {
        cooldowns.remove(skillId);
    }

    /**
     * 모든 쿨타임을 초기화합니다.
     */
    public void resetAllCooldowns() {
        cooldowns.clear();
    }

    /**
     * 스킬 트리를 초기화합니다. (리스펙)
     */
    public void resetSkillTree() {
        availablePoints += totalInvestedPoints;
        totalInvestedPoints = 0;
        skillRanks.clear();
    }

    /**
     * 스킬이 해금되었는지 확인합니다.
     */
    public boolean isSkillUnlocked(String skillId) {
        return skillRanks.getOrDefault(skillId, 0) > 0;
    }

    /**
     * 모든 투자된 스킬을 반환합니다.
     */
    public Map<String, Integer> getAllSkillRanks() {
        return Map.copyOf(skillRanks);
    }
}
