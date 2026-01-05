package com.sanctuary.combat.event;

import com.sanctuary.core.script.ScriptEngine;
import org.luaj.vm2.LuaValue;
import org.luaj.vm2.LuaTable;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * 전투 이벤트 버스
 * 전투 관련 이벤트의 중앙 분배 시스템입니다.
 * 
 * Java 리스너와 Lua 훅(Hook)을 모두 지원합니다.
 * 
 * 사용 예시:
 * 
 * <pre>
 * // Java 리스너 등록
 * eventBus.register(CombatEventType.DAMAGE_DEALT, event -> {
 *     DamageDealtEvent damage = (DamageDealtEvent) event;
 *     logger.info("Damage: " + damage.getFinalDamage());
 * });
 * 
 * // Lua 훅 등록
 * eventBus.registerLuaHook(CombatEventType.CRITICAL_HIT, "onCriticalHit");
 * 
 * // 이벤트 발생
 * eventBus.fire(new DamageDealtEvent(context, 100.0));
 * </pre>
 */
public class CombatEventBus {

    private final Logger logger;
    private final ScriptEngine scriptEngine;

    // Java 리스너 저장소
    private final Map<CombatEventType, List<CombatEventListener>> listeners = new ConcurrentHashMap<>();

    // Lua 훅 저장소 (이벤트 타입 -> Lua 함수 이름 목록)
    private final Map<CombatEventType, List<String>> luaHooks = new ConcurrentHashMap<>();

    // 전역 리스너 (모든 이벤트 수신)
    private final List<CombatEventListener> globalListeners = new CopyOnWriteArrayList<>();

    // 이벤트 통계 (디버깅용)
    private final Map<CombatEventType, Long> eventCounts = new ConcurrentHashMap<>();

    public CombatEventBus(Logger logger, ScriptEngine scriptEngine) {
        this.logger = logger;
        this.scriptEngine = scriptEngine;
    }

    // ===== Java 리스너 관리 =====

    /**
     * 특정 이벤트 타입에 리스너를 등록합니다.
     * 
     * @param type     이벤트 타입
     * @param listener 리스너
     */
    public void register(CombatEventType type, CombatEventListener listener) {
        listeners.computeIfAbsent(type, k -> new CopyOnWriteArrayList<>()).add(listener);
        logger.fine("[CombatEventBus] 리스너 등록: " + type);
    }

    /**
     * 특정 이벤트 타입에서 리스너를 제거합니다.
     */
    public void unregister(CombatEventType type, CombatEventListener listener) {
        List<CombatEventListener> list = listeners.get(type);
        if (list != null) {
            list.remove(listener);
        }
    }

    /**
     * 모든 이벤트를 수신하는 전역 리스너를 등록합니다.
     */
    public void registerGlobal(CombatEventListener listener) {
        globalListeners.add(listener);
    }

    /**
     * 전역 리스너를 제거합니다.
     */
    public void unregisterGlobal(CombatEventListener listener) {
        globalListeners.remove(listener);
    }

    // ===== Lua 훅 관리 =====

    /**
     * Lua 훅을 등록합니다.
     * 해당 이벤트 발생 시 지정된 Lua 함수가 호출됩니다.
     * 
     * @param type            이벤트 타입
     * @param luaFunctionName Lua 전역 함수 이름
     */
    public void registerLuaHook(CombatEventType type, String luaFunctionName) {
        luaHooks.computeIfAbsent(type, k -> new CopyOnWriteArrayList<>()).add(luaFunctionName);
        logger.fine("[CombatEventBus] Lua 훅 등록: " + type + " -> " + luaFunctionName);
    }

    /**
     * Lua 훅을 제거합니다.
     */
    public void unregisterLuaHook(CombatEventType type, String luaFunctionName) {
        List<String> hooks = luaHooks.get(type);
        if (hooks != null) {
            hooks.remove(luaFunctionName);
        }
    }

    /**
     * 모든 Lua 훅을 제거합니다.
     */
    public void clearLuaHooks() {
        luaHooks.clear();
        logger.info("[CombatEventBus] 모든 Lua 훅 제거됨");
    }

    // ===== 이벤트 발생 =====

    /**
     * 이벤트를 발생시키고 모든 리스너에게 전달합니다.
     * 
     * @param event 발생시킬 이벤트
     * @return 이벤트가 취소되었는지 여부
     */
    public boolean fire(CombatEvent event) {
        CombatEventType type = event.getType();

        // 통계 업데이트
        eventCounts.merge(type, 1L, Long::sum);

        // 1. 전역 리스너 호출
        for (CombatEventListener listener : globalListeners) {
            try {
                listener.onEvent(event);
            } catch (Exception e) {
                logger.log(Level.WARNING, "[CombatEventBus] 전역 리스너 오류: " + e.getMessage(), e);
            }
        }

        // 2. 타입별 Java 리스너 호출
        List<CombatEventListener> typeListeners = listeners.get(type);
        if (typeListeners != null) {
            for (CombatEventListener listener : typeListeners) {
                try {
                    listener.onEvent(event);
                } catch (Exception e) {
                    logger.log(Level.WARNING, "[CombatEventBus] 리스너 오류: " + e.getMessage(), e);
                }
            }
        }

        // 3. Lua 훅 호출
        List<String> hooks = luaHooks.get(type);
        if (hooks != null && scriptEngine != null) {
            LuaTable eventTable = eventToLuaTable(event);
            for (String hookName : hooks) {
                try {
                    scriptEngine.callFunction(hookName, eventTable);
                } catch (Exception e) {
                    logger.log(Level.WARNING, "[CombatEventBus] Lua 훅 오류 (" + hookName + "): " + e.getMessage(), e);
                }
            }
        }

        return event.isCancelled();
    }

    /**
     * 이벤트를 발생시키고 완료 후 콜백을 실행합니다.
     */
    public void fire(CombatEvent event, Consumer<CombatEvent> callback) {
        fire(event);
        if (callback != null) {
            callback.accept(event);
        }
    }

    // ===== 유틸리티 =====

    /**
     * CombatEvent를 Lua 테이블로 변환합니다.
     */
    private LuaTable eventToLuaTable(CombatEvent event) {
        LuaTable table = new LuaTable();

        table.set("type", event.getType().name());
        table.set("cancelled", LuaValue.valueOf(event.isCancelled()));

        // 컨텍스트 정보
        CombatContext ctx = event.getContext();
        if (ctx != null) {
            LuaTable ctxTable = new LuaTable();
            if (ctx.getAttacker() != null) {
                ctxTable.set("attackerId", ctx.getAttacker().getUuid().toString());
            }
            if (ctx.getVictim() != null) {
                ctxTable.set("victimId", ctx.getVictim().getUuid().toString());
            }
            if (ctx.getSkillId() != null) {
                ctxTable.set("skillId", ctx.getSkillId());
            }
            ctxTable.set("skillCoefficient", ctx.getSkillCoefficient());
            ctxTable.set("distance", ctx.getDistance());

            // 태그
            LuaTable tags = new LuaTable();
            int i = 1;
            for (String tag : ctx.getTags()) {
                tags.set(i++, tag);
            }
            ctxTable.set("tags", tags);

            table.set("context", ctxTable);
        }

        // DamageDealtEvent 특수 처리
        if (event instanceof DamageDealtEvent) {
            DamageDealtEvent dde = (DamageDealtEvent) event;
            table.set("baseDamage", dde.getBaseDamage());
            table.set("finalDamage", dde.getFinalDamage());
            table.set("damageType", dde.getDamageType());
            table.set("isCrit", LuaValue.valueOf(dde.isCritical()));
            table.set("isOverpower", LuaValue.valueOf(dde.isOverpower()));
            table.set("isVuln", LuaValue.valueOf(dde.isVulnerable()));
            table.set("isLuckyHit", LuaValue.valueOf(dde.isLuckyHit()));
        }

        return table;
    }

    /**
     * 이벤트 통계를 반환합니다.
     */
    public Map<CombatEventType, Long> getEventCounts() {
        return new HashMap<>(eventCounts);
    }

    /**
     * 등록된 리스너 수를 반환합니다.
     */
    public int getListenerCount(CombatEventType type) {
        List<CombatEventListener> list = listeners.get(type);
        return list != null ? list.size() : 0;
    }

    /**
     * 모든 리스너와 훅을 제거합니다.
     */
    public void clear() {
        listeners.clear();
        luaHooks.clear();
        globalListeners.clear();
        eventCounts.clear();
        logger.info("[CombatEventBus] 모든 리스너 및 훅 제거됨");
    }
}
