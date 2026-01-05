package com.sanctuary.core.world;

import org.bukkit.World;
import org.bukkit.entity.Player;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * 월드 티어 관리자
 * 디아블로 4의 월드 티어 시스템을 구현합니다.
 * 
 * 월드 티어:
 * 1 (어드벤처) - 기본, 1.0x 보상
 * 2 (베테랑) - 중급, 1.25x 보상, 적 +20% HP
 * 3 (악몽) - 서정 등급, 1.5x 보상, 적 +50% HP, 악몽 던전 해금
 * 4 (고문) - 최상위, 2.0x 보상, 적 +100% HP, 조상 아이템 드롭
 */
public class WorldTierManager {

    // 월드별 티어 설정
    private final Map<String, Integer> worldTiers = new HashMap<>();

    // 플레이어 개인 월드 티어 오버라이드
    private final Map<UUID, Integer> playerTierOverrides = new HashMap<>();

    // 월드 티어 스탯
    public static final int MIN_TIER = 1;
    public static final int MAX_TIER = 4;

    // 월드 티어별 보정값
    private static final Map<Integer, TierStats> TIER_STATS = Map.of(
            1, new TierStats(1.0, 1.0, 0, "어드벤처"),
            2, new TierStats(1.25, 1.2, 10, "베테랑"),
            3, new TierStats(1.5, 1.5, 50, "악몽"),
            4, new TierStats(2.0, 2.0, 100, "고문"));

    /**
     * 기본 세계 티어를 설정합니다.
     */
    public void setWorldTier(World world, int tier) {
        setWorldTier(world.getName(), tier);
    }

    public void setWorldTier(String worldName, int tier) {
        tier = Math.max(MIN_TIER, Math.min(MAX_TIER, tier));
        worldTiers.put(worldName, tier);
    }

    /**
     * 세계의 티어를 반환합니다.
     */
    public int getWorldTier(World world) {
        return getWorldTier(world.getName());
    }

    public int getWorldTier(String worldName) {
        return worldTiers.getOrDefault(worldName, 1);
    }

    /**
     * 플레이어의 유효 월드 티어를 반환합니다.
     * 개인 오버라이드가 있으면 해당 값, 없으면 세계 티어 반환
     */
    public int getPlayerWorldTier(Player player) {
        UUID playerId = player.getUniqueId();
        if (playerTierOverrides.containsKey(playerId)) {
            return playerTierOverrides.get(playerId);
        }
        return getWorldTier(player.getWorld());
    }

    /**
     * 플레이어 개인 월드 티어를 설정합니다.
     */
    public void setPlayerWorldTier(Player player, int tier) {
        tier = Math.max(MIN_TIER, Math.min(MAX_TIER, tier));
        playerTierOverrides.put(player.getUniqueId(), tier);
    }

    /**
     * 플레이어 개인 월드 티어 오버라이드를 제거합니다.
     */
    public void clearPlayerWorldTier(Player player) {
        playerTierOverrides.remove(player.getUniqueId());
    }

    /**
     * 월드 티어별 보상 배율을 반환합니다.
     */
    public double getRewardMultiplier(int tier) {
        TierStats stats = TIER_STATS.get(tier);
        return stats != null ? stats.rewardMultiplier : 1.0;
    }

    /**
     * 월드 티어별 적 체력 배율을 반환합니다.
     */
    public double getEnemyHealthMultiplier(int tier) {
        TierStats stats = TIER_STATS.get(tier);
        return stats != null ? stats.enemyHealthMultiplier : 1.0;
    }

    /**
     * 월드 티어별 최소 레벨을 반환합니다.
     */
    public int getMinimumLevel(int tier) {
        TierStats stats = TIER_STATS.get(tier);
        return stats != null ? stats.minimumLevel : 0;
    }

    /**
     * 월드 티어 이름을 반환합니다.
     */
    public String getTierName(int tier) {
        TierStats stats = TIER_STATS.get(tier);
        return stats != null ? stats.name : "알 수 없음";
    }

    /**
     * 플레이어가 해당 월드 티어에 접근 가능한지 확인합니다.
     */
    public boolean canAccessTier(Player player, int tier, int playerLevel) {
        int minLevel = getMinimumLevel(tier);
        return playerLevel >= minLevel;
    }

    /**
     * 월드 티어 스탯 레코드
     */
    public record TierStats(
            double rewardMultiplier,
            double enemyHealthMultiplier,
            int minimumLevel,
            String name) {
    }
}
