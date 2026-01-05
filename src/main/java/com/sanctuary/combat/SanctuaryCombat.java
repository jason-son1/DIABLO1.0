package com.sanctuary.combat;

import com.sanctuary.DiabloPlugin;
import com.sanctuary.combat.calc.DamageCalculator;
import com.sanctuary.combat.calc.DefenseCalculator;
import com.sanctuary.combat.event.CombatEventBus;
import com.sanctuary.combat.listener.DamageListener;
import com.sanctuary.combat.paragon.ParagonBoardManager;
import com.sanctuary.combat.paragon.ParagonCommand;
import com.sanctuary.combat.skill.SkillExecutor;
import com.sanctuary.combat.skill.SkillTreeCommand;
import com.sanctuary.combat.skill.SkillTreeManager;
import com.sanctuary.combat.stat.StatManager;
import com.sanctuary.combat.status.StatusEffectManager;
import com.sanctuary.core.SanctuaryCore;
import com.sanctuary.items.SanctuaryItems;
import org.bukkit.scheduler.BukkitRunnable;

/**
 * SanctuaryCombat (심장)
 * 역할: 데미지 버킷 연산, 스탯 관리, 상태 이상 처리, 전투 이벤트 파이프라인
 */
public class SanctuaryCombat {

    private final DiabloPlugin plugin;
    private final SanctuaryCore core;
    private final SanctuaryItems items;

    private StatManager statManager;
    private DamageCalculator damageCalculator;
    private DefenseCalculator defenseCalculator;
    private StatusEffectManager statusEffectManager;
    private CombatEventBus eventBus;

    // Phase 3: 스킬 트리 & 정복자 보드
    private SkillTreeManager skillTreeManager;
    private SkillExecutor skillExecutor;
    private ParagonBoardManager paragonBoardManager;

    // 상태 이상 틱 태스크
    private BukkitRunnable statusTickTask;
    private BukkitRunnable dotDamageTask;

    public SanctuaryCombat(DiabloPlugin plugin, SanctuaryCore core, SanctuaryItems items) {
        this.plugin = plugin;
        this.core = core;
        this.items = items;
    }

    public void initialize() {
        plugin.getLogger().info("[SanctuaryCombat] 초기화 중...");

        // 1. 전투 이벤트 버스 초기화 (가장 먼저)
        this.eventBus = new CombatEventBus(plugin.getLogger(), core.getScriptEngine());

        // 2. 매니저 및 계산기 초기화
        this.statManager = new StatManager(core);
        this.damageCalculator = new DamageCalculator(core.getScriptEngine(), eventBus);
        this.defenseCalculator = new DefenseCalculator();
        this.statusEffectManager = new StatusEffectManager(plugin.getLogger());

        // 3. 스킬 트리 & 정복자 보드 초기화
        this.skillTreeManager = new SkillTreeManager(plugin.getLogger(), core.getEntityManager(),
                core.getDataRepository());
        this.skillExecutor = new SkillExecutor(plugin.getLogger(), skillTreeManager, core.getEntityManager(),
                core.getScriptEngine());
        this.paragonBoardManager = new ParagonBoardManager(plugin.getLogger(), core.getEntityManager());

        // 4. 이벤트 리스너 등록
        DamageListener damageListener = new DamageListener(
                statManager,
                damageCalculator,
                defenseCalculator,
                statusEffectManager,
                eventBus);
        plugin.getServer().getPluginManager().registerEvents(damageListener, plugin);

        // 5. 명령어 등록
        registerCommands();

        // 6. 상태 이상 틱 태스크 시작
        startStatusTickTask();

        // 7. DoT 피해 태스크 시작 (1초마다)
        startDoTDamageTask();

        // 8. Lua 전투 스크립트 로드
        loadCombatScripts();

        plugin.getLogger().info("[SanctuaryCombat] 데미지 파이프라인 초기화 완료.");
    }

    /**
     * 명령어를 등록합니다.
     */
    private void registerCommands() {
        plugin.getCommand("skilltree").setExecutor(new SkillTreeCommand(this, skillTreeManager));
        plugin.getCommand("paragon").setExecutor(new ParagonCommand(this, paragonBoardManager));
        plugin.getLogger().info("[SanctuaryCombat] 스킬트리/정복자 명령어 등록 완료.");
    }

    /**
     * 상태 이상 지속시간을 처리하는 틱 태스크를 시작합니다.
     * 매 틱(50ms)마다 실행됩니다.
     */
    private void startStatusTickTask() {
        statusTickTask = new BukkitRunnable() {
            @Override
            public void run() {
                statusEffectManager.update();
            }
        };
        statusTickTask.runTaskTimer(plugin, 1L, 1L); // 매 틱
    }

    /**
     * DoT(지속 피해)를 처리하는 태스크를 시작합니다.
     * 매 초(20틱)마다 실행됩니다.
     */
    private void startDoTDamageTask() {
        dotDamageTask = new BukkitRunnable() {
            @Override
            public void run() {
                // 모든 플레이어의 DoT 처리
                for (var player : plugin.getServer().getOnlinePlayers()) {
                    double dotDamage = statusEffectManager.processDoTDamage(player);
                    if (dotDamage > 0) {
                        double currentHp = player.getHealth();
                        double newHp = Math.max(0.5, currentHp - dotDamage); // 최소 0.5 유지
                        player.setHealth(newHp);

                        // TODO: DoT 피해 인디케이터 표시
                    }
                }
            }
        };
        dotDamageTask.runTaskTimer(plugin, 20L, 20L); // 매 초
    }

    /**
     * Lua 전투 스크립트를 로드합니다.
     */
    private void loadCombatScripts() {
        if (core.getScriptEngine() != null) {
            core.getScriptEngine().loadScript("damage_calculator.lua");
            core.getScriptEngine().loadScript("combat_effects.lua");
            core.getScriptEngine().loadScript("skills.lua");
            plugin.getLogger().info("[SanctuaryCombat] Lua 전투/스킬 스크립트 로드 완료.");
        }
    }

    public void shutdown() {
        // 태스크 취소
        if (statusTickTask != null) {
            statusTickTask.cancel();
        }
        if (dotDamageTask != null) {
            dotDamageTask.cancel();
        }

        // 이벤트 버스 정리
        if (eventBus != null) {
            eventBus.clear();
        }

        // 상태 이상 정리
        if (statusEffectManager != null) {
            statusEffectManager.clear();
        }

        plugin.getLogger().info("[SanctuaryCombat] 시스템 종료됨.");
    }

    // ===== Getters =====

    public SanctuaryCore getCore() {
        return core;
    }

    public DiabloPlugin getPlugin() {
        return plugin;
    }

    public StatManager getStatManager() {
        return statManager;
    }

    public DamageCalculator getDamageCalculator() {
        return damageCalculator;
    }

    public DefenseCalculator getDefenseCalculator() {
        return defenseCalculator;
    }

    public StatusEffectManager getStatusEffectManager() {
        return statusEffectManager;
    }

    public CombatEventBus getEventBus() {
        return eventBus;
    }

    public SkillTreeManager getSkillTreeManager() {
        return skillTreeManager;
    }

    public SkillExecutor getSkillExecutor() {
        return skillExecutor;
    }

    public ParagonBoardManager getParagonBoardManager() {
        return paragonBoardManager;
    }
}
