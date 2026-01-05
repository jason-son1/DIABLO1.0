package com.sanctuary.world.dungeon;

import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;

import java.time.Instant;
import java.util.*;

/**
 * 던전 인스턴스
 * 파티별로 생성되는 독립된 던전 세션입니다.
 */
public class DungeonInstance {

    public enum State {
        LOADING, // 월드 로딩 중
        READY, // 입장 대기
        IN_PROGRESS, // 진행 중
        COMPLETED, // 클리어
        FAILED, // 실패
        CLOSING // 정리 중
    }

    private final String instanceId;
    private final DungeonTemplate template;
    private final int tier;
    private final UUID ownerId;

    // 상태
    private State state = State.LOADING;
    private Instant startTime;
    private Instant endTime;

    // 월드 참조
    private World instanceWorld;
    private Location spawnLocation;

    // 파티원
    private final Set<UUID> players = new HashSet<>();
    private final Set<UUID> deadPlayers = new HashSet<>();

    // 진행 상황
    private int totalEnemies = 0;
    private int killedEnemies = 0;
    private boolean bossSpawned = false;
    private boolean bossKilled = false;

    // 보상
    private final List<String> lootPool = new ArrayList<>();
    private double earnedGold = 0;
    private double earnedXp = 0;

    public DungeonInstance(String instanceId, DungeonTemplate template, int tier, UUID ownerId) {
        this.instanceId = instanceId;
        this.template = template;
        this.tier = tier;
        this.ownerId = ownerId;
    }

    // ===== 라이프사이클 =====

    /**
     * 던전을 시작합니다.
     */
    public void start() {
        this.state = State.IN_PROGRESS;
        this.startTime = Instant.now();
    }

    /**
     * 던전 클리어 처리
     */
    public void complete() {
        this.state = State.COMPLETED;
        this.endTime = Instant.now();
    }

    /**
     * 던전 실패 처리
     */
    public void fail(String reason) {
        this.state = State.FAILED;
        this.endTime = Instant.now();
    }

    /**
     * 던전 정리
     */
    public void close() {
        this.state = State.CLOSING;
    }

    // ===== 플레이어 관리 =====

    public void addPlayer(UUID playerId) {
        players.add(playerId);
    }

    public void removePlayer(UUID playerId) {
        players.remove(playerId);
    }

    public boolean hasPlayer(UUID playerId) {
        return players.contains(playerId);
    }

    public void markPlayerDead(UUID playerId) {
        deadPlayers.add(playerId);
    }

    public boolean isAllPlayersDead() {
        return deadPlayers.containsAll(players);
    }

    // ===== 진행 상황 =====

    public void onEnemyKilled() {
        killedEnemies++;
        if (killedEnemies >= totalEnemies && !bossSpawned) {
            // TODO: 보스 스폰 트리거
        }
    }

    public void onBossKilled() {
        bossKilled = true;
        complete();
    }

    public double getProgress() {
        if (totalEnemies == 0)
            return 0;
        return (double) killedEnemies / totalEnemies;
    }

    // ===== 보상 =====

    public void addLoot(String itemId) {
        lootPool.add(itemId);
    }

    public void addGold(double amount) {
        earnedGold += amount * template.getGoldMultiplier() * template.calculateRewardMultiplier(tier);
    }

    public void addXp(double amount) {
        earnedXp += amount * template.getXpMultiplier() * template.calculateRewardMultiplier(tier);
    }

    // ===== 시간 =====

    public long getElapsedSeconds() {
        if (startTime == null)
            return 0;
        Instant end = endTime != null ? endTime : Instant.now();
        return end.getEpochSecond() - startTime.getEpochSecond();
    }

    public boolean isTimedOut() {
        if (template.getTimeLimit() <= 0)
            return false;
        return getElapsedSeconds() >= template.getTimeLimit();
    }

    // ===== Getters =====

    public String getInstanceId() {
        return instanceId;
    }

    public DungeonTemplate getTemplate() {
        return template;
    }

    public int getTier() {
        return tier;
    }

    public UUID getOwnerId() {
        return ownerId;
    }

    public State getState() {
        return state;
    }

    public void setState(State state) {
        this.state = state;
    }

    public World getInstanceWorld() {
        return instanceWorld;
    }

    public void setInstanceWorld(World instanceWorld) {
        this.instanceWorld = instanceWorld;
    }

    public Location getSpawnLocation() {
        return spawnLocation;
    }

    public void setSpawnLocation(Location spawnLocation) {
        this.spawnLocation = spawnLocation;
    }

    public Set<UUID> getPlayers() {
        return Set.copyOf(players);
    }

    public int getPlayerCount() {
        return players.size();
    }

    public int getTotalEnemies() {
        return totalEnemies;
    }

    public void setTotalEnemies(int totalEnemies) {
        this.totalEnemies = totalEnemies;
    }

    public int getKilledEnemies() {
        return killedEnemies;
    }

    public boolean isBossKilled() {
        return bossKilled;
    }

    public List<String> getLootPool() {
        return lootPool;
    }

    public double getEarnedGold() {
        return earnedGold;
    }

    public double getEarnedXp() {
        return earnedXp;
    }
}
