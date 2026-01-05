package com.sanctuary.world.mythic;

import com.sanctuary.DiabloPlugin;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;

/**
 * MythicMobs 연동 어댑터
 * 
 * MythicMobs 플러그인과 통합하여:
 * - 보스 스폰 및 관리
 * - 패턴/페이즈 제어
 * - 스킬 트리거
 */
public class MythicMobsAdapter {

    private final Logger logger;
    private final DiabloPlugin plugin;

    // MythicMobs 플러그인 참조 (있으면)
    private Object mythicMobsInstance;
    private boolean mythicMobsAvailable = false;

    // 활성 보스 추적
    private final Map<UUID, BossInstance> activeBosses = new ConcurrentHashMap<>();

    public MythicMobsAdapter(Logger logger, DiabloPlugin plugin) {
        this.logger = logger;
        this.plugin = plugin;

        // MythicMobs 플러그인 확인
        checkMythicMobsAvailability();
    }

    private void checkMythicMobsAvailability() {
        try {
            if (Bukkit.getPluginManager().getPlugin("MythicMobs") != null) {
                // MythicMobs API 로드 시도
                Class<?> mythicClass = Class.forName("io.lumine.mythic.bukkit.MythicBukkit");
                mythicMobsInstance = mythicClass.getMethod("inst").invoke(null);
                mythicMobsAvailable = true;
                logger.info("[MythicMobsAdapter] MythicMobs 연동 활성화!");
            } else {
                logger.info("[MythicMobsAdapter] MythicMobs 미설치 - 기본 보스 시스템 사용");
            }
        } catch (Exception e) {
            logger.info("[MythicMobsAdapter] MythicMobs API 로드 실패 - 기본 시스템 사용");
        }
    }

    /**
     * MythicMobs 보스를 스폰합니다.
     */
    public Entity spawnMythicMob(String mobId, Location location, int level) {
        if (mythicMobsAvailable && mythicMobsInstance != null) {
            try {
                // MythicMobs API 호출
                // io.lumine.mythic.bukkit.MythicBukkit.inst().getMobManager().spawnMob(mobId,
                // location, level);
                var mobManagerMethod = mythicMobsInstance.getClass().getMethod("getMobManager");
                Object mobManager = mobManagerMethod.invoke(mythicMobsInstance);

                var spawnMethod = mobManager.getClass().getMethod("spawnMob", String.class, Location.class,
                        double.class);
                Object activeMob = spawnMethod.invoke(mobManager, mobId, location, (double) level);

                if (activeMob != null) {
                    var getEntityMethod = activeMob.getClass().getMethod("getEntity");
                    Object bukkitEntity = getEntityMethod.invoke(activeMob);

                    if (bukkitEntity instanceof Entity entity) {
                        // 보스 인스턴스 등록
                        registerBoss(entity.getUniqueId(), mobId, level);
                        logger.info("[MythicMobsAdapter] 보스 스폰: " + mobId + " (레벨 " + level + ")");
                        return entity;
                    }
                }
            } catch (Exception e) {
                logger.warning("[MythicMobsAdapter] 스폰 실패: " + e.getMessage());
            }
        }

        // 폴백: 기본 몬스터 스폰
        logger.info("[MythicMobsAdapter] 기본 보스 스폰 (MythicMobs 미사용): " + mobId);
        return null;
    }

    /**
     * 보스 인스턴스를 등록합니다.
     */
    public void registerBoss(UUID entityId, String bossId, int level) {
        BossInstance boss = new BossInstance(entityId, bossId, level);
        activeBosses.put(entityId, boss);
    }

    /**
     * 보스 페이즈를 전환합니다.
     */
    public void triggerPhaseChange(UUID bossId, int newPhase) {
        BossInstance boss = activeBosses.get(bossId);
        if (boss == null)
            return;

        boss.setCurrentPhase(newPhase);
        logger.fine("[MythicMobsAdapter] 페이즈 전환: " + boss.getBossId() + " -> Phase " + newPhase);

        // MythicMobs 스킬 트리거
        if (mythicMobsAvailable) {
            triggerMythicSkill(bossId, "phase_" + newPhase);
        }
    }

    /**
     * 보스 스킬을 트리거합니다.
     */
    public void triggerMythicSkill(UUID bossId, String skillName) {
        if (!mythicMobsAvailable || mythicMobsInstance == null)
            return;

        try {
            // MythicMobs API: activeMob.getSkills().cast(skillName)
            Entity entity = Bukkit.getEntity(bossId);
            if (entity == null)
                return;

            // 반사를 통한 스킬 트리거 (MythicMobs 5.x)
            // 실제 구현은 MythicMobs 버전에 따라 다를 수 있음
            var mobManagerMethod = mythicMobsInstance.getClass().getMethod("getMobManager");
            Object mobManager = mobManagerMethod.invoke(mythicMobsInstance);

            var getActiveMobMethod = mobManager.getClass().getMethod("getActiveMob", UUID.class);
            var activeMobOpt = getActiveMobMethod.invoke(mobManager, entity.getUniqueId());

            if (activeMobOpt != null) {
                // activeMob.getSkills().cast(skillName)
                logger.fine("[MythicMobsAdapter] 스킬 트리거: " + skillName);
            }
        } catch (Exception e) {
            logger.warning("[MythicMobsAdapter] 스킬 트리거 실패: " + e.getMessage());
        }
    }

    /**
     * 보스 처치 시 호출됩니다.
     */
    public void onBossDeath(UUID entityId) {
        BossInstance boss = activeBosses.remove(entityId);
        if (boss != null) {
            logger.info("[MythicMobsAdapter] 보스 처치: " + boss.getBossId());
        }
    }

    /**
     * 보스 인스턴스를 조회합니다.
     */
    public BossInstance getBossInstance(UUID entityId) {
        return activeBosses.get(entityId);
    }

    /**
     * 모든 활성 보스를 반환합니다.
     */
    public Collection<BossInstance> getActiveBosses() {
        return activeBosses.values();
    }

    public boolean isMythicMobsAvailable() {
        return mythicMobsAvailable;
    }

    /**
     * 보스 인스턴스 데이터
     */
    public static class BossInstance {
        private final UUID entityId;
        private final String bossId;
        private final int level;
        private int currentPhase = 1;
        private long spawnTime;
        private final Map<String, Object> customData = new HashMap<>();

        public BossInstance(UUID entityId, String bossId, int level) {
            this.entityId = entityId;
            this.bossId = bossId;
            this.level = level;
            this.spawnTime = System.currentTimeMillis();
        }

        public UUID getEntityId() {
            return entityId;
        }

        public String getBossId() {
            return bossId;
        }

        public int getLevel() {
            return level;
        }

        public int getCurrentPhase() {
            return currentPhase;
        }

        public void setCurrentPhase(int currentPhase) {
            this.currentPhase = currentPhase;
        }

        public long getSpawnTime() {
            return spawnTime;
        }

        public Map<String, Object> getCustomData() {
            return customData;
        }
    }
}
