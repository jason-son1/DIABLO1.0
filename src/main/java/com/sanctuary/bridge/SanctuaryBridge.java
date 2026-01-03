package com.sanctuary.bridge;

import com.sanctuary.DiabloPlugin;
import com.sanctuary.bridge.command.BridgeTestCommand;
import com.sanctuary.bridge.listener.BridgeEventListener;
import com.sanctuary.bridge.packet.PacketManager;
import com.sanctuary.bridge.packet.PacketType;
import com.sanctuary.bridge.sync.StatSyncService;
import org.bukkit.command.PluginCommand;
import org.bukkit.scheduler.BukkitRunnable;

/**
 * SanctuaryBridge (신경망)
 * 역할: 네트워크 패킷 처리, 클라이언트 UI 통신 (SDUI)
 */
public class SanctuaryBridge {

    private final DiabloPlugin plugin;

    private PacketManager packetManager;
    private StatSyncService statSyncService;

    // 동기화 태스크
    private BukkitRunnable syncTask;

    public SanctuaryBridge(DiabloPlugin plugin) {
        this.plugin = plugin;
    }

    public void initialize() {
        plugin.getLogger().info("[SanctuaryBridge] 초기화 중...");

        // 1. 패킷 매니저 초기화
        this.packetManager = new PacketManager(plugin, plugin.getLogger());
        this.packetManager.initialize();

        // 2. 동기화 서비스 초기화
        this.statSyncService = new StatSyncService(
                packetManager,
                plugin.getCombatModule().getStatManager(),
                plugin.getCombatModule().getStatusEffectManager(),
                plugin.getLogger());

        // 3. 패킷 핸들러 등록
        registerPacketHandlers();

        // 4. 이벤트 리스너 등록
        plugin.getServer().getPluginManager().registerEvents(
                new BridgeEventListener(this),
                plugin);

        // 5. 주기적 동기화 태스크 시작
        startSyncTask();

        // 6. 테스트 명령어 등록
        registerCommands();

        plugin.getLogger().info("[SanctuaryBridge] 네트워킹 시스템 초기화 완료.");
    }

    private void registerPacketHandlers() {
        // 스킬 사용 요청 핸들러
        packetManager.registerHandler(PacketType.C2S_SKILL_CAST, ctx -> {
            String skillId = ctx.getData("skillId");
            plugin.getLogger().info("[Bridge] 스킬 사용 요청: " + skillId + " from " + ctx.player().getName());
            // TODO: SkillCastService 연동
        });

        // UI 상호작용 핸들러
        packetManager.registerHandler(PacketType.C2S_UI_INTERACTION, ctx -> {
            String componentId = ctx.getData("componentId");
            String action = ctx.getData("action");
            plugin.getLogger().info("[Bridge] UI 상호작용: " + componentId + " / " + action);
        });
    }

    /**
     * 주기적으로 모드 클라이언트에 데이터를 동기화합니다.
     */
    private void startSyncTask() {
        syncTask = new BukkitRunnable() {
            @Override
            public void run() {
                for (var player : plugin.getServer().getOnlinePlayers()) {
                    if (packetManager.hasModClient(player)) {
                        statSyncService.syncStats(player);
                        statSyncService.syncStatusEffects(player);
                    }
                }
            }
        };
        syncTask.runTaskTimer(plugin, 20L, 10L); // 0.5초마다
    }

    private void registerCommands() {
        PluginCommand bridgeCmd = plugin.getCommand("bridgetest");
        if (bridgeCmd != null) {
            BridgeTestCommand handler = new BridgeTestCommand(plugin, this);
            bridgeCmd.setExecutor(handler);
            bridgeCmd.setTabCompleter(handler);
        }
    }

    public void shutdown() {
        if (syncTask != null) {
            syncTask.cancel();
        }
        if (packetManager != null) {
            packetManager.shutdown();
        }
        plugin.getLogger().info("[SanctuaryBridge] 시스템 종료됨.");
    }

    // ===== Getters =====

    public PacketManager getPacketManager() {
        return packetManager;
    }

    public StatSyncService getStatSyncService() {
        return statSyncService;
    }

    public DiabloPlugin getPlugin() {
        return plugin;
    }
}
