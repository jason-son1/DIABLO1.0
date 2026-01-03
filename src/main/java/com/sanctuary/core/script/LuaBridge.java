package com.sanctuary.core.script;

import com.sanctuary.core.ecs.SanctuaryEntity;
import com.sanctuary.core.ecs.component.AttributeComponent;
import com.sanctuary.core.ecs.component.TagComponent;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.luaj.vm2.Globals;
import org.luaj.vm2.LuaValue;
import org.luaj.vm2.lib.OneArgFunction;
import org.luaj.vm2.lib.TwoArgFunction;
import org.luaj.vm2.lib.ZeroArgFunction;
import java.util.logging.Logger;

/**
 * Java ↔ Lua 바인딩 API를 제공하는 클래스입니다.
 * Lua 스크립트에서 게임 기능을 안전하게 호출할 수 있도록 합니다.
 * 
 * Lua에서 사용 예시:
 * 
 * <pre>
 * local strength = sanctuary.getStat(entity, "STRENGTH")
 * sanctuary.playSound(entity, "ENTITY_GENERIC_EXPLODE")
 * </pre>
 */
public class LuaBridge {

    private final Logger logger;
    private final com.sanctuary.core.ecs.EntityManager entityManager;

    public LuaBridge(Logger logger) {
        this.logger = logger;
        this.entityManager = null;
    }

    public LuaBridge(Logger logger, com.sanctuary.core.ecs.EntityManager entityManager) {
        this.logger = logger;
        this.entityManager = entityManager;
    }

    /**
     * Lua Globals에 Sanctuary API를 등록합니다.
     * 
     * @param globals Lua Globals 객체
     */
    public void registerAPI(Globals globals) {
        LuaValue sanctuaryLib = LuaValue.tableOf();

        // 로깅 함수
        sanctuaryLib.set("log", new LogFunction());
        sanctuaryLib.set("warn", new WarnFunction());

        // 스탯 조회 함수 (향후 SanctuaryEntity 연동)
        sanctuaryLib.set("getStat", new GetStatFunction());

        // 태그 확인 함수
        sanctuaryLib.set("hasTag", new HasTagFunction());

        // 사운드 재생 함수
        sanctuaryLib.set("playSound", new PlaySoundFunction());

        globals.set("sanctuary", sanctuaryLib);

        logger.info("[LuaBridge] Sanctuary API 등록 완료.");
    }

    /**
     * 샌드박싱을 적용합니다.
     * 위험한 Lua 모듈을 제거합니다.
     * 
     * @param globals Lua Globals 객체
     */
    public void applySandbox(Globals globals) {
        // 파일 시스템 접근 제거
        globals.set("io", LuaValue.NIL);
        globals.set("os", LuaValue.NIL);
        globals.set("debug", LuaValue.NIL);
        globals.set("loadfile", LuaValue.NIL);
        globals.set("dofile", LuaValue.NIL);

        logger.info("[LuaBridge] 샌드박싱 적용 완료 (io, os, debug 제거).");
    }

    // ===== Lua 함수 구현 =====

    /**
     * sanctuary.log(message) - 정보 로그 출력
     */
    private class LogFunction extends OneArgFunction {
        @Override
        public LuaValue call(LuaValue arg) {
            logger.info("[Lua] " + arg.tojstring());
            return LuaValue.NIL;
        }
    }

    /**
     * sanctuary.warn(message) - 경고 로그 출력
     */
    private class WarnFunction extends OneArgFunction {
        @Override
        public LuaValue call(LuaValue arg) {
            logger.warning("[Lua] " + arg.tojstring());
            return LuaValue.NIL;
        }
    }

    /**
     * sanctuary.getStat(entityTable, statKey) - 스탯 조회
     * EntityManager에서 엔티티를 조회하고 AttributeComponent에서 스탯을 반환합니다.
     */
    private class GetStatFunction extends TwoArgFunction {
        @Override
        public LuaValue call(LuaValue entityArg, LuaValue statKey) {
            String key = statKey.tojstring();

            // entityArg에서 UUID 추출
            if (!entityArg.istable()) {
                logger.warning("[Lua] getStat: 잘못된 엔티티 형식");
                return LuaValue.valueOf(0.0);
            }

            String uuidStr = entityArg.get("uuid").tojstring();
            if (uuidStr == null || entityManager == null) {
                return LuaValue.valueOf(0.0);
            }

            try {
                java.util.UUID uuid = java.util.UUID.fromString(uuidStr);
                SanctuaryEntity entity = entityManager.get(uuid);
                if (entity != null) {
                    return entity.getComponentOptional(AttributeComponent.class)
                            .map(attr -> LuaValue.valueOf(attr.getValue(key)))
                            .orElse(LuaValue.valueOf(0.0));
                }
            } catch (IllegalArgumentException e) {
                logger.warning("[Lua] getStat: 잘못된 UUID 형식: " + uuidStr);
            }

            return LuaValue.valueOf(0.0);
        }
    }

    /**
     * sanctuary.hasTag(entityTable, tag) - 태그 확인
     * EntityManager에서 엔티티를 조회하고 TagComponent에서 태그를 확인합니다.
     */
    private class HasTagFunction extends TwoArgFunction {
        @Override
        public LuaValue call(LuaValue entityArg, LuaValue tagArg) {
            String tag = tagArg.tojstring();

            if (!entityArg.istable() || entityManager == null) {
                return LuaValue.FALSE;
            }

            String uuidStr = entityArg.get("uuid").tojstring();
            if (uuidStr == null) {
                return LuaValue.FALSE;
            }

            try {
                java.util.UUID uuid = java.util.UUID.fromString(uuidStr);
                SanctuaryEntity entity = entityManager.get(uuid);
                if (entity != null) {
                    return entity.getComponentOptional(TagComponent.class)
                            .map(tags -> LuaValue.valueOf(tags.has(tag)))
                            .orElse(LuaValue.FALSE);
                }
            } catch (IllegalArgumentException e) {
                logger.warning("[Lua] hasTag: 잘못된 UUID 형식: " + uuidStr);
            }

            return LuaValue.FALSE;
        }
    }

    /**
     * sanctuary.playSound(entityTable, soundName) - 사운드 재생
     * EntityManager에서 엔티티를 조회하여 해당 위치에서 사운드를 재생합니다.
     */
    private class PlaySoundFunction extends TwoArgFunction {
        @Override
        public LuaValue call(LuaValue entityArg, LuaValue soundArg) {
            String soundName = soundArg.tojstring();

            try {
                Sound sound = Sound.valueOf(soundName);

                if (entityArg.istable() && entityManager != null) {
                    String uuidStr = entityArg.get("uuid").tojstring();
                    if (uuidStr != null) {
                        java.util.UUID uuid = java.util.UUID.fromString(uuidStr);
                        SanctuaryEntity entity = entityManager.get(uuid);
                        if (entity != null && entity.getBukkitEntity() != null) {
                            org.bukkit.Location loc = entity.getBukkitEntity().getLocation();
                            loc.getWorld().playSound(loc, sound, 1.0f, 1.0f);
                            return LuaValue.TRUE;
                        }
                    }
                }

                return LuaValue.TRUE;
            } catch (IllegalArgumentException e) {
                logger.warning("[Lua] playSound: 알 수 없는 사운드: " + soundName);
                return LuaValue.FALSE;
            }
        }
    }

    // ===== 유틸리티 메서드 =====

    /**
     * SanctuaryEntity를 Lua 테이블로 변환합니다.
     * 
     * @param entity SanctuaryEntity
     * @return Lua 테이블
     */
    public LuaValue entityToLua(SanctuaryEntity entity) {
        LuaValue table = LuaValue.tableOf();
        table.set("uuid", entity.getUuid().toString());

        // AttributeComponent가 있으면 스탯 추가
        entity.getComponentOptional(AttributeComponent.class).ifPresent(attr -> {
            LuaValue stats = LuaValue.tableOf();
            for (String key : attr.getKeys()) {
                stats.set(key, attr.getValue(key));
            }
            table.set("stats", stats);
        });

        // TagComponent가 있으면 태그 추가
        entity.getComponentOptional(TagComponent.class).ifPresent(tags -> {
            LuaValue tagTable = LuaValue.tableOf();
            int i = 1;
            for (String tag : tags.getAll()) {
                tagTable.set(i++, tag);
            }
            table.set("tags", tagTable);
        });

        return table;
    }
}
