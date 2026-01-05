package com.sanctuary.world.event;

import com.sanctuary.DiabloPlugin;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Monster;
import org.bukkit.entity.Player;

import java.util.*;
import java.util.logging.Logger;

/**
 * 지옥물결(Helltide) 관리자
 * 
 * 특징:
 * - 서버 시간 기반으로 특정 지역의 몬스터 강화
 * - 잉걸불(Aberrant Cinder) 드롭 활성화
 * - 특수 상자 및 보스 스폰
 */
public class HelltideManager implements WorldEventManager.WorldEventHandler {

    private final Logger logger;
    private final DiabloPlugin plugin;
    private final WorldEventManager eventManager;

    // 설정
    private int durationMinutes = 60; // 지속 시간
    private int cooldownMinutes = 75; // 재발동 대기
    private int monsterLevelBonus = 5; // 몬스터 레벨 보너스
    private double lootMultiplier = 1.5; // 보상 배율
    private double cinderDropRate = 0.3; // 잉걸불 드롭 확률

    // 활성 구역
    private final Map<String, HelltideZone> zones = new HashMap<>();
    private String currentActiveZone = null;

    // 타이머
    private long lastHelltideEnd = 0;

    public HelltideManager(Logger logger, DiabloPlugin plugin, WorldEventManager eventManager) {
        this.logger = logger;
        this.plugin = plugin;
        this.eventManager = eventManager;

        // 핸들러 등록
        eventManager.registerHandler("HELLTIDE", this);

        // 기본 구역 정의
        defineDefaultZones();
    }

    /**
     * 기본 지옥물결 구역을 정의합니다.
     */
    private void defineDefaultZones() {
        // 월드 "world"의 특정 영역들
        zones.put("fractured_peaks", new HelltideZone(
                "fractured_peaks",
                "부서진 봉우리",
                "world",
                new int[] { -500, -500, 500, 500 } // minX, minZ, maxX, maxZ
        ));

        zones.put("scosglen", new HelltideZone(
                "scosglen",
                "스코스글렌",
                "world",
                new int[] { 500, -500, 1500, 500 }));

        zones.put("dry_steppes", new HelltideZone(
                "dry_steppes",
                "건조한 대초원",
                "world",
                new int[] { -1500, 500, -500, 1500 }));

        zones.put("kehjistan", new HelltideZone(
                "kehjistan",
                "케지스탄",
                "world",
                new int[] { 500, 500, 1500, 1500 }));

        logger.info("[HelltideManager] " + zones.size() + "개 구역 정의됨");
    }

    /**
     * 지옥물결을 시작합니다.
     */
    public void startHelltide(String zoneId) {
        if (currentActiveZone != null) {
            logger.warning("[HelltideManager] 이미 활성화된 지옥물결이 있습니다.");
            return;
        }

        HelltideZone zone = zones.get(zoneId);
        if (zone == null) {
            logger.warning("[HelltideManager] 존재하지 않는 구역: " + zoneId);
            return;
        }

        // 쿨다운 확인
        long now = System.currentTimeMillis();
        long cooldownMs = cooldownMinutes * 60 * 1000L;
        if (now - lastHelltideEnd < cooldownMs) {
            long remaining = (cooldownMs - (now - lastHelltideEnd)) / 1000 / 60;
            logger.info("[HelltideManager] 쿨다운 중: " + remaining + "분 남음");
            return;
        }

        // 이벤트 파라미터
        Map<String, Object> params = new HashMap<>();
        params.put("zoneId", zoneId);
        params.put("zoneName", zone.getName());
        params.put("worldName", zone.getWorldName());
        params.put("bounds", zone.getBounds());

        String eventId = "helltide_" + System.currentTimeMillis();
        if (eventManager.startEvent(eventId, "HELLTIDE", params)) {
            WorldEventManager.WorldEvent event = eventManager.getEvent(eventId);
            event.setDurationSeconds(durationMinutes * 60);
            currentActiveZone = zoneId;

            // 서버 전체 알림
            Bukkit.broadcastMessage("§4§l[지옥물결]§r §c" + zone.getName() + "§7에 지옥물결이 시작되었습니다!");
            Bukkit.broadcastMessage("§7지옥물결은 §e" + durationMinutes + "분§7 동안 지속됩니다.");
        }
    }

    /**
     * 지옥물결을 종료합니다.
     */
    public void endHelltide() {
        if (currentActiveZone != null) {
            HelltideZone zone = zones.get(currentActiveZone);
            Bukkit.broadcastMessage("§4§l[지옥물결]§r §7" + zone.getName() + "의 지옥물결이 종료되었습니다.");

            // 이벤트 종료
            var events = eventManager.getActiveEventsByType("HELLTIDE");
            for (var event : events) {
                eventManager.endEvent(event.getEventId());
            }
        }
    }

    // ===== WorldEventHandler 구현 =====

    @Override
    public void onStart(WorldEventManager.WorldEvent event) {
        String zoneId = event.getParam("zoneId", "");
        logger.info("[HelltideManager] 지옥물결 시작: " + zoneId);

        // 해당 구역의 몬스터 강화
        strengthenMonstersInZone(zoneId);
    }

    @Override
    public void onTick(WorldEventManager.WorldEvent event) {
        // 매 초마다 호출됨
        String zoneId = event.getParam("zoneId", "");

        // 구역 내 새로운 몬스터 강화
        strengthenMonstersInZone(zoneId);

        // 10분마다 알림
        long elapsed = event.getElapsedSeconds();
        int remaining = (durationMinutes * 60) - (int) elapsed;
        if (remaining > 0 && remaining % 600 == 0) {
            Bukkit.broadcastMessage("§4§l[지옥물결]§r §7남은 시간: §e" + (remaining / 60) + "분");
        }
    }

    @Override
    public void onEnd(WorldEventManager.WorldEvent event) {
        String zoneId = event.getParam("zoneId", "");
        logger.info("[HelltideManager] 지옥물결 종료: " + zoneId);

        currentActiveZone = null;
        lastHelltideEnd = System.currentTimeMillis();
    }

    /**
     * 구역 내 몬스터를 강화합니다.
     */
    private void strengthenMonstersInZone(String zoneId) {
        HelltideZone zone = zones.get(zoneId);
        if (zone == null)
            return;

        World world = Bukkit.getWorld(zone.getWorldName());
        if (world == null)
            return;

        int[] bounds = zone.getBounds();
        for (Entity entity : world.getEntities()) {
            if (entity instanceof Monster monster) {
                Location loc = monster.getLocation();
                if (loc.getX() >= bounds[0] && loc.getX() <= bounds[2] &&
                        loc.getZ() >= bounds[1] && loc.getZ() <= bounds[3]) {

                    // 이미 강화됨 체크
                    if (!monster.hasMetadata("helltide_buffed")) {
                        // 체력 증가
                        double newHealth = monster.getHealth() * 1.5;
                        monster.setHealth(Math.min(newHealth, monster.getMaxHealth()));

                        // 메타데이터 설정
                        monster.setMetadata("helltide_buffed",
                                new org.bukkit.metadata.FixedMetadataValue(plugin, true));
                        monster.setMetadata("helltide_level_bonus",
                                new org.bukkit.metadata.FixedMetadataValue(plugin, monsterLevelBonus));
                    }
                }
            }
        }
    }

    /**
     * 잉걸불 드롭 확인
     */
    public boolean shouldDropCinder(LivingEntity entity) {
        if (currentActiveZone == null)
            return false;

        HelltideZone zone = zones.get(currentActiveZone);
        if (zone == null)
            return false;

        Location loc = entity.getLocation();
        int[] bounds = zone.getBounds();

        if (loc.getX() >= bounds[0] && loc.getX() <= bounds[2] &&
                loc.getZ() >= bounds[1] && loc.getZ() <= bounds[3]) {
            return Math.random() < cinderDropRate;
        }

        return false;
    }

    /**
     * 플레이어가 지옥물결 구역에 있는지 확인
     */
    public boolean isInHelltideZone(Player player) {
        if (currentActiveZone == null)
            return false;

        HelltideZone zone = zones.get(currentActiveZone);
        if (zone == null)
            return false;

        Location loc = player.getLocation();
        if (!loc.getWorld().getName().equals(zone.getWorldName()))
            return false;

        int[] bounds = zone.getBounds();
        return loc.getX() >= bounds[0] && loc.getX() <= bounds[2] &&
                loc.getZ() >= bounds[1] && loc.getZ() <= bounds[3];
    }

    // ===== Getters & Setters =====

    public String getCurrentActiveZone() {
        return currentActiveZone;
    }

    public boolean isActive() {
        return currentActiveZone != null;
    }

    public double getLootMultiplier() {
        return lootMultiplier;
    }

    public int getMonsterLevelBonus() {
        return monsterLevelBonus;
    }

    /**
     * 지옥물결 구역 정의
     */
    public static class HelltideZone {
        private final String id;
        private final String name;
        private final String worldName;
        private final int[] bounds; // minX, minZ, maxX, maxZ

        public HelltideZone(String id, String name, String worldName, int[] bounds) {
            this.id = id;
            this.name = name;
            this.worldName = worldName;
            this.bounds = bounds;
        }

        public String getId() {
            return id;
        }

        public String getName() {
            return name;
        }

        public String getWorldName() {
            return worldName;
        }

        public int[] getBounds() {
            return bounds;
        }
    }
}
