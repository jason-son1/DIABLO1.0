package com.sanctuary.core;

import com.sanctuary.DiabloPlugin;
import com.sanctuary.core.command.SanctuaryCommand;
import com.sanctuary.core.data.DataRepository;
import com.sanctuary.core.data.JsonDataLoader;
import com.sanctuary.core.ecs.EntityManager;
import com.sanctuary.core.script.ScriptEngine;
import org.bukkit.command.PluginCommand;

/**
 * SanctuaryCore (시스템의 두뇌)
 * 역할: 데이터 로드(ETL), ECS(엔티티 컴포넌트) 관리, Lua 스크립트 엔진
 */
public class SanctuaryCore {

    private final DiabloPlugin plugin;
    private DataRepository dataRepository;
    private ScriptEngine scriptEngine;
    private EntityManager entityManager;

    public SanctuaryCore(DiabloPlugin plugin) {
        this.plugin = plugin;
    }

    public void initialize() {
        plugin.getLogger().info("SanctuaryCore 초기화 중...");

        // 1. 데이터 로더 초기화
        this.dataRepository = new JsonDataLoader(plugin.getDataFolder(), plugin.getLogger());
        this.dataRepository.reload();

        // 2. 스크립트 엔진 초기화
        this.scriptEngine = new ScriptEngine(plugin.getDataFolder(), plugin.getLogger());

        // 3. ECS 엔티티 매니저 초기화
        this.entityManager = new EntityManager(plugin.getLogger());

        // 4. 리스너 등록
        registerListeners();

        // 5. 명령어 등록
        registerCommands();
    }

    private void registerListeners() {
        plugin.getServer().getPluginManager().registerEvents(
                new com.sanctuary.core.listener.EntityListener(this), plugin);
    }

    private void registerCommands() {
        PluginCommand sanctuaryCmd = plugin.getCommand("sanctuary");
        if (sanctuaryCmd != null) {
            SanctuaryCommand handler = new SanctuaryCommand(plugin, this);
            sanctuaryCmd.setExecutor(handler);
            sanctuaryCmd.setTabCompleter(handler);
        } else {
            plugin.getLogger().warning("'sanctuary' 명령어가 plugin.yml에 정의되지 않았습니다.");
        }
    }

    /**
     * 모든 데이터와 스크립트를 리로드합니다.
     */
    public void reload() {
        plugin.getLogger().info("[SanctuaryCore] 시스템 리로드 중...");

        // 데이터 리로드
        dataRepository.reload();

        // 스크립트 캐시 초기화
        scriptEngine.reloadAll();

        plugin.getLogger().info("[SanctuaryCore] 시스템 리로드 완료.");
    }

    public void shutdown() {
        // 엔티티 매니저 정리
        if (entityManager != null) {
            entityManager.clear();
        }
        plugin.getLogger().info("[SanctuaryCore] 시스템 종료됨.");
    }

    // ===== Getters =====

    public DataRepository getDataRepository() {
        return dataRepository;
    }

    public ScriptEngine getScriptEngine() {
        return scriptEngine;
    }

    public EntityManager getEntityManager() {
        return entityManager;
    }

    public DiabloPlugin getPlugin() {
        return plugin;
    }
}
