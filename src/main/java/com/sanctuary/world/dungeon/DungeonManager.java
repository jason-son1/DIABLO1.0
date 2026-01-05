package com.sanctuary.world.dungeon;

import com.sanctuary.DiabloPlugin;
import org.bukkit.*;
import org.bukkit.entity.Player;

import java.io.File;
import java.io.IOException;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;

/**
 * 던전 관리자
 * 던전 템플릿 로드, 인스턴스 생성/삭제, 파티 관리를 담당합니다.
 */
public class DungeonManager {

    private final Logger logger;
    private final DiabloPlugin plugin;

    // 던전 템플릿 캐시
    private final Map<String, DungeonTemplate> templates = new ConcurrentHashMap<>();

    // 활성 던전 인스턴스
    private final Map<String, DungeonInstance> instances = new ConcurrentHashMap<>();

    // 플레이어 → 던전 매핑
    private final Map<UUID, String> playerDungeonMap = new ConcurrentHashMap<>();

    // 인스턴스 ID 카운터
    private int instanceCounter = 0;

    public DungeonManager(Logger logger, DiabloPlugin plugin) {
        this.logger = logger;
        this.plugin = plugin;
    }

    /**
     * 던전 템플릿을 등록합니다.
     */
    public void registerTemplate(DungeonTemplate template) {
        templates.put(template.getId(), template);
        logger.info("[DungeonManager] 템플릿 등록: " + template.getId());
    }

    /**
     * 던전 인스턴스를 생성합니다.
     */
    public DungeonInstance createInstance(String templateId, int tier, Player owner) {
        DungeonTemplate template = templates.get(templateId);
        if (template == null) {
            logger.warning("[DungeonManager] 존재하지 않는 템플릿: " + templateId);
            return null;
        }

        // 티어 범위 확인
        tier = Math.max(template.getMinTier(), Math.min(tier, template.getMaxTier()));

        // 인스턴스 ID 생성
        String instanceId = templateId + "_" + (++instanceCounter) + "_" + System.currentTimeMillis();

        // 인스턴스 생성
        DungeonInstance instance = new DungeonInstance(instanceId, template, tier, owner.getUniqueId());
        instance.addPlayer(owner.getUniqueId());
        instances.put(instanceId, instance);
        playerDungeonMap.put(owner.getUniqueId(), instanceId);

        // 월드 생성 (비동기)
        createInstanceWorld(instance);

        logger.info("[DungeonManager] 던전 생성: " + instanceId + " (티어 " + tier + ")");
        return instance;
    }

    /**
     * 인스턴스 월드를 생성합니다.
     */
    private void createInstanceWorld(DungeonInstance instance) {
        String templateWorldName = instance.getTemplate().getTemplateWorld();
        String instanceWorldName = "dungeon_" + instance.getInstanceId();

        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            try {
                // 템플릿 월드 폴더 복사
                File sourceDir = new File(Bukkit.getWorldContainer(), templateWorldName);
                File destDir = new File(Bukkit.getWorldContainer(), instanceWorldName);

                if (!sourceDir.exists()) {
                    logger.warning("[DungeonManager] 템플릿 월드 없음: " + templateWorldName);
                    instance.setState(DungeonInstance.State.FAILED);
                    return;
                }

                copyDirectory(sourceDir.toPath(), destDir.toPath());

                // 메인 스레드에서 월드 로드
                Bukkit.getScheduler().runTask(plugin, () -> {
                    World world = Bukkit.createWorld(new WorldCreator(instanceWorldName));
                    if (world != null) {
                        world.setAutoSave(false);
                        world.setKeepSpawnInMemory(false);
                        instance.setInstanceWorld(world);

                        // 스폰 위치 설정
                        String spawnStr = instance.getTemplate().getSpawnLocation();
                        if (spawnStr != null && !spawnStr.isEmpty()) {
                            String[] parts = spawnStr.split(",");
                            double x = Double.parseDouble(parts[0]);
                            double y = Double.parseDouble(parts[1]);
                            double z = Double.parseDouble(parts[2]);
                            instance.setSpawnLocation(new Location(world, x, y, z));
                        } else {
                            instance.setSpawnLocation(world.getSpawnLocation());
                        }

                        instance.setState(DungeonInstance.State.READY);
                        logger.info("[DungeonManager] 인스턴스 월드 준비 완료: " + instanceWorldName);
                    }
                });

            } catch (IOException e) {
                logger.severe("[DungeonManager] 월드 복사 실패: " + e.getMessage());
                instance.setState(DungeonInstance.State.FAILED);
            }
        });
    }

    /**
     * 플레이어를 던전에 입장시킵니다.
     */
    public boolean enterDungeon(Player player, String instanceId) {
        DungeonInstance instance = instances.get(instanceId);
        if (instance == null || instance.getState() != DungeonInstance.State.READY) {
            return false;
        }

        // 기존 던전에서 퇴장
        leaveDungeon(player);

        // 입장
        instance.addPlayer(player.getUniqueId());
        playerDungeonMap.put(player.getUniqueId(), instanceId);
        player.teleport(instance.getSpawnLocation());

        // 첫 입장 시 던전 시작
        if (instance.getState() == DungeonInstance.State.READY) {
            instance.start();
        }

        logger.info("[DungeonManager] " + player.getName() + " 던전 입장: " + instanceId);
        return true;
    }

    /**
     * 플레이어를 던전에서 퇴장시킵니다.
     */
    public void leaveDungeon(Player player) {
        String instanceId = playerDungeonMap.remove(player.getUniqueId());
        if (instanceId != null) {
            DungeonInstance instance = instances.get(instanceId);
            if (instance != null) {
                instance.removePlayer(player.getUniqueId());

                // 플레이어가 없으면 던전 종료
                if (instance.getPlayerCount() == 0) {
                    closeInstance(instanceId);
                }
            }
        }

        // 메인 월드로 이동
        World mainWorld = Bukkit.getWorlds().get(0);
        player.teleport(mainWorld.getSpawnLocation());
    }

    /**
     * 인스턴스를 종료합니다.
     */
    public void closeInstance(String instanceId) {
        DungeonInstance instance = instances.remove(instanceId);
        if (instance == null)
            return;

        instance.close();

        // 월드 언로드 및 삭제
        World world = instance.getInstanceWorld();
        if (world != null) {
            String worldName = world.getName();

            // 모든 플레이어 퇴장
            for (Player player : world.getPlayers()) {
                player.teleport(Bukkit.getWorlds().get(0).getSpawnLocation());
            }

            // 월드 언로드
            Bukkit.unloadWorld(world, false);

            // 폴더 삭제 (비동기)
            Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
                try {
                    deleteDirectory(new File(Bukkit.getWorldContainer(), worldName).toPath());
                    logger.info("[DungeonManager] 인스턴스 삭제: " + instanceId);
                } catch (IOException e) {
                    logger.warning("[DungeonManager] 폴더 삭제 실패: " + e.getMessage());
                }
            });
        }
    }

    /**
     * 플레이어의 현재 던전을 반환합니다.
     */
    public DungeonInstance getPlayerDungeon(UUID playerId) {
        String instanceId = playerDungeonMap.get(playerId);
        return instanceId != null ? instances.get(instanceId) : null;
    }

    /**
     * 던전 인스턴스를 반환합니다.
     */
    public DungeonInstance getInstance(String instanceId) {
        return instances.get(instanceId);
    }

    /**
     * 모든 활성 던전을 종료합니다.
     */
    public void shutdown() {
        for (String instanceId : new ArrayList<>(instances.keySet())) {
            closeInstance(instanceId);
        }
        logger.info("[DungeonManager] 모든 던전 종료됨.");
    }

    // ===== 유틸리티 =====

    private void copyDirectory(Path source, Path target) throws IOException {
        Files.walk(source).forEach(path -> {
            try {
                Path dest = target.resolve(source.relativize(path));
                if (Files.isDirectory(path)) {
                    Files.createDirectories(dest);
                } else {
                    // uid.dat는 복사하지 않음 (고유 월드 ID)
                    if (!path.getFileName().toString().equals("uid.dat")) {
                        Files.copy(path, dest, StandardCopyOption.REPLACE_EXISTING);
                    }
                }
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        });
    }

    private void deleteDirectory(Path directory) throws IOException {
        if (Files.exists(directory)) {
            Files.walk(directory)
                    .sorted(Comparator.reverseOrder())
                    .forEach(path -> {
                        try {
                            Files.delete(path);
                        } catch (IOException e) {
                            // 무시
                        }
                    });
        }
    }
}
