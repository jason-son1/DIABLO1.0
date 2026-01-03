package com.sanctuary;

import com.sanctuary.bridge.SanctuaryBridge;
import com.sanctuary.combat.SanctuaryCombat;
import com.sanctuary.core.SanctuaryCore;
import com.sanctuary.items.SanctuaryItems;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.logging.Logger;

/**
 * Project Sanctuary (DIABLO) - 메인 플러그인 클래스
 * 4개의 핵심 모듈(Core, Combat, Items, Bridge)을 관리합니다.
 */
public class DiabloPlugin extends JavaPlugin {

    private static DiabloPlugin instance;
    private Logger logger;

    // 핵심 모듈
    private SanctuaryCore coreModule;
    private SanctuaryCombat combatModule;
    private SanctuaryItems itemsModule;
    private SanctuaryBridge bridgeModule;

    @Override
    public void onEnable() {
        instance = this;
        this.logger = getLogger();

        logger.info("[Sanctuary] Sanctuary Dev Team :: 시스템 초기화 시작...");

        try {
            // 1. Core 모듈 초기화 (데이터, ECS)
            this.coreModule = new SanctuaryCore(this);
            this.coreModule.initialize();
            logger.info("[Sanctuary] Core 모듈 활성화 완료.");

            // 2. Items 모듈 초기화 (아이템 생성, NBT)
            this.itemsModule = new SanctuaryItems(this, coreModule);
            this.itemsModule.initialize();
            logger.info("[Sanctuary] Items 모듈 활성화 완료.");

            // 3. Combat 모듈 초기화 (데미지 파이프라인)
            this.combatModule = new SanctuaryCombat(this, coreModule, itemsModule);
            this.combatModule.initialize();
            logger.info("[Sanctuary] Combat 모듈 활성화 완료.");

            // 4. Bridge 모듈 초기화 (네트워킹)
            this.bridgeModule = new SanctuaryBridge(this);
            this.bridgeModule.initialize();
            logger.info("[Sanctuary] Bridge 모듈 활성화 완료.");

            logger.info("[Sanctuary] 모든 시스템이 정상적으로 가동되었습니다. 성역에 오신 것을 환영합니다.");

        } catch (Exception e) {
            logger.severe("[Sanctuary] 시스템 초기화 중 치명적인 오류 발생!");
            e.printStackTrace();
            getServer().getPluginManager().disablePlugin(this);
        }
    }

    @Override
    public void onDisable() {
        logger.info("[Sanctuary] 시스템 종료 중...");
        
        if (bridgeModule != null) bridgeModule.shutdown();
        if (combatModule != null) combatModule.shutdown();
        if (itemsModule != null) itemsModule.shutdown();
        if (coreModule != null) coreModule.shutdown();

        logger.info("[Sanctuary] 성역의 문이 닫혔습니다.");
    }

    public static DiabloPlugin getInstance() {
        return instance;
    }

    public SanctuaryCore getCoreModule() { return coreModule; }
    public SanctuaryCombat getCombatModule() { return combatModule; }
    public SanctuaryItems getItemsModule() { return itemsModule; }
    public SanctuaryBridge getBridgeModule() { return bridgeModule; }
}
