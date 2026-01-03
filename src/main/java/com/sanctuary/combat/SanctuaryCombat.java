package com.sanctuary.combat;

import com.sanctuary.DiabloPlugin;
import com.sanctuary.combat.calc.DamageCalculator;
import com.sanctuary.combat.listener.DamageListener;
import com.sanctuary.combat.stat.StatManager;
import com.sanctuary.core.SanctuaryCore;
import com.sanctuary.items.SanctuaryItems;

/**
 * SanctuaryCombat (심장)
 * 역할: 데미지 버킷 연산, 스탯 관리, 상태 이상 처리
 */
public class SanctuaryCombat {

    private final DiabloPlugin plugin;
    private final SanctuaryCore core;
    private final SanctuaryItems items;

    public SanctuaryCombat(DiabloPlugin plugin, SanctuaryCore core, SanctuaryItems items) {
        this.plugin = plugin;
        this.core = core;
        this.items = items;
    }

    private StatManager statManager;
    private DamageCalculator damageCalculator;

    public void initialize() {
        // 1. 매니저 및 계산기 초기화
        this.statManager = new StatManager();
        this.damageCalculator = new DamageCalculator();

        // 2. 이벤트 리스너 등록
        DamageListener damageListener = new DamageListener(statManager, damageCalculator);
        plugin.getServer().getPluginManager().registerEvents(damageListener, plugin);

        plugin.getLogger().info("[SanctuaryCombat] 데미지 파이프라인(StatManager, Calculator, Listener) 초기화 완료.");
    }

    public StatManager getStatManager() {
        return statManager;
    }

    public void shutdown() {

    }
}
