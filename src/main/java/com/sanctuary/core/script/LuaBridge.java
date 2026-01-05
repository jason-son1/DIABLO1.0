package com.sanctuary.core.script;

import com.sanctuary.core.ecs.SanctuaryEntity;
import com.sanctuary.core.ecs.component.AttributeComponent;
import com.sanctuary.core.ecs.component.StateComponent;
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

        // ===== 로깅 =====
        sanctuaryLib.set("log", new LogFunction());
        sanctuaryLib.set("warn", new WarnFunction());

        // ===== 엔티티 스탯/태그 =====
        sanctuaryLib.set("getStat", new GetStatFunction());
        sanctuaryLib.set("hasTag", new HasTagFunction());
        sanctuaryLib.set("addTag", new AddTagFunction());
        sanctuaryLib.set("removeTag", new RemoveTagFunction());

        // ===== 엔티티 체력 =====
        sanctuaryLib.set("getHealth", new GetHealthFunction());
        sanctuaryLib.set("setHealth", new SetHealthFunction());
        sanctuaryLib.set("getMaxHealth", new GetMaxHealthFunction());
        sanctuaryLib.set("heal", new HealFunction());
        sanctuaryLib.set("damage", new DamageFunction());

        // ===== 보강 (Fortify) =====
        sanctuaryLib.set("getFortify", new GetFortifyFunction());
        sanctuaryLib.set("setFortify", new SetFortifyFunction());
        sanctuaryLib.set("addFortify", new AddFortifyFunction());
        sanctuaryLib.set("isFortified", new IsFortifiedFunction());

        // ===== 보호막 (Barrier) =====
        sanctuaryLib.set("getBarrier", new GetBarrierFunction());
        sanctuaryLib.set("setBarrier", new SetBarrierFunction());
        sanctuaryLib.set("addBarrier", new AddBarrierFunction());
        sanctuaryLib.set("hasBarrier", new HasBarrierFunction());

        // ===== 이펙트 =====
        sanctuaryLib.set("playSound", new PlaySoundFunction());
        sanctuaryLib.set("spawnParticle", new SpawnParticleFunction());

        globals.set("sanctuary", sanctuaryLib);

        logger.info("[LuaBridge] Sanctuary API 등록 완료 (확장 API 포함).");
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

    // ===== 태그 확장 함수 =====

    /**
     * sanctuary.addTag(entityTable, tag) - 태그 추가
     */
    private class AddTagFunction extends TwoArgFunction {
        @Override
        public LuaValue call(LuaValue entityArg, LuaValue tagArg) {
            SanctuaryEntity entity = getEntityFromArg(entityArg);
            if (entity == null)
                return LuaValue.FALSE;

            TagComponent tags = entity.getComponent(TagComponent.class);
            if (tags == null) {
                tags = new TagComponent();
                entity.attach(tags);
            }
            tags.add(tagArg.tojstring());
            return LuaValue.TRUE;
        }
    }

    /**
     * sanctuary.removeTag(entityTable, tag) - 태그 제거
     */
    private class RemoveTagFunction extends TwoArgFunction {
        @Override
        public LuaValue call(LuaValue entityArg, LuaValue tagArg) {
            SanctuaryEntity entity = getEntityFromArg(entityArg);
            if (entity == null)
                return LuaValue.FALSE;

            return entity.getComponentOptional(TagComponent.class)
                    .map(tags -> {
                        tags.remove(tagArg.tojstring());
                        return LuaValue.TRUE;
                    })
                    .orElse(LuaValue.FALSE);
        }
    }

    // ===== 체력 관련 함수 =====

    /**
     * sanctuary.getHealth(entityTable) - 현재 체력 조회
     */
    private class GetHealthFunction extends OneArgFunction {
        @Override
        public LuaValue call(LuaValue entityArg) {
            SanctuaryEntity entity = getEntityFromArg(entityArg);
            if (entity == null || entity.getBukkitEntity() == null)
                return LuaValue.valueOf(0.0);

            if (entity.getBukkitEntity() instanceof org.bukkit.entity.LivingEntity living) {
                return LuaValue.valueOf(living.getHealth());
            }
            return LuaValue.valueOf(0.0);
        }
    }

    /**
     * sanctuary.setHealth(entityTable, health) - 체력 설정
     */
    private class SetHealthFunction extends TwoArgFunction {
        @Override
        public LuaValue call(LuaValue entityArg, LuaValue healthArg) {
            SanctuaryEntity entity = getEntityFromArg(entityArg);
            if (entity == null || entity.getBukkitEntity() == null)
                return LuaValue.FALSE;

            if (entity.getBukkitEntity() instanceof org.bukkit.entity.LivingEntity living) {
                double health = Math.max(0, healthArg.todouble());
                double maxHealth = living.getMaxHealth();
                living.setHealth(Math.min(health, maxHealth));
                return LuaValue.TRUE;
            }
            return LuaValue.FALSE;
        }
    }

    /**
     * sanctuary.getMaxHealth(entityTable) - 최대 체력 조회
     */
    private class GetMaxHealthFunction extends OneArgFunction {
        @Override
        public LuaValue call(LuaValue entityArg) {
            SanctuaryEntity entity = getEntityFromArg(entityArg);
            if (entity == null || entity.getBukkitEntity() == null)
                return LuaValue.valueOf(0.0);

            if (entity.getBukkitEntity() instanceof org.bukkit.entity.LivingEntity living) {
                return LuaValue.valueOf(living.getMaxHealth());
            }
            return LuaValue.valueOf(0.0);
        }
    }

    /**
     * sanctuary.heal(entityTable, amount) - 체력 회복
     */
    private class HealFunction extends TwoArgFunction {
        @Override
        public LuaValue call(LuaValue entityArg, LuaValue amountArg) {
            SanctuaryEntity entity = getEntityFromArg(entityArg);
            if (entity == null || entity.getBukkitEntity() == null)
                return LuaValue.FALSE;

            if (entity.getBukkitEntity() instanceof org.bukkit.entity.LivingEntity living) {
                double amount = amountArg.todouble();
                double newHealth = Math.min(living.getHealth() + amount, living.getMaxHealth());
                living.setHealth(newHealth);
                return LuaValue.valueOf(newHealth);
            }
            return LuaValue.FALSE;
        }
    }

    /**
     * sanctuary.damage(entityTable, amount) - 피해 적용
     */
    private class DamageFunction extends TwoArgFunction {
        @Override
        public LuaValue call(LuaValue entityArg, LuaValue amountArg) {
            SanctuaryEntity entity = getEntityFromArg(entityArg);
            if (entity == null || entity.getBukkitEntity() == null)
                return LuaValue.FALSE;

            if (entity.getBukkitEntity() instanceof org.bukkit.entity.LivingEntity living) {
                double amount = amountArg.todouble();
                double newHealth = Math.max(0, living.getHealth() - amount);
                living.setHealth(newHealth);
                return LuaValue.valueOf(newHealth);
            }
            return LuaValue.FALSE;
        }
    }

    // ===== 보강(Fortify) 함수 =====

    /**
     * sanctuary.getFortify(entityTable) - 보강 수치 조회
     */
    private class GetFortifyFunction extends OneArgFunction {
        @Override
        public LuaValue call(LuaValue entityArg) {
            SanctuaryEntity entity = getEntityFromArg(entityArg);
            if (entity == null)
                return LuaValue.valueOf(0.0);

            return entity.getComponentOptional(StateComponent.class)
                    .map(state -> LuaValue.valueOf(state.getFortifyAmount()))
                    .orElse(LuaValue.valueOf(0.0));
        }
    }

    /**
     * sanctuary.setFortify(entityTable, amount) - 보강 수치 설정
     */
    private class SetFortifyFunction extends TwoArgFunction {
        @Override
        public LuaValue call(LuaValue entityArg, LuaValue amountArg) {
            SanctuaryEntity entity = getEntityFromArg(entityArg);
            if (entity == null)
                return LuaValue.FALSE;

            StateComponent state = getOrCreateStateComponent(entity);
            state.setFortifyAmount(amountArg.todouble());
            return LuaValue.TRUE;
        }
    }

    /**
     * sanctuary.addFortify(entityTable, amount) - 보강 수치 추가
     */
    private class AddFortifyFunction extends TwoArgFunction {
        @Override
        public LuaValue call(LuaValue entityArg, LuaValue amountArg) {
            SanctuaryEntity entity = getEntityFromArg(entityArg);
            if (entity == null)
                return LuaValue.FALSE;

            StateComponent state = getOrCreateStateComponent(entity);
            state.addFortify(amountArg.todouble());
            return LuaValue.valueOf(state.getFortifyAmount());
        }
    }

    /**
     * sanctuary.isFortified(entityTable) - 보강 활성화 여부 확인
     */
    private class IsFortifiedFunction extends OneArgFunction {
        @Override
        public LuaValue call(LuaValue entityArg) {
            SanctuaryEntity entity = getEntityFromArg(entityArg);
            if (entity == null || entity.getBukkitEntity() == null)
                return LuaValue.FALSE;

            if (entity.getBukkitEntity() instanceof org.bukkit.entity.LivingEntity living) {
                return entity.getComponentOptional(StateComponent.class)
                        .map(state -> LuaValue.valueOf(state.isFortified(living.getHealth())))
                        .orElse(LuaValue.FALSE);
            }
            return LuaValue.FALSE;
        }
    }

    // ===== 보호막(Barrier) 함수 =====

    /**
     * sanctuary.getBarrier(entityTable) - 보호막 수치 조회
     */
    private class GetBarrierFunction extends OneArgFunction {
        @Override
        public LuaValue call(LuaValue entityArg) {
            SanctuaryEntity entity = getEntityFromArg(entityArg);
            if (entity == null)
                return LuaValue.valueOf(0.0);

            return entity.getComponentOptional(StateComponent.class)
                    .map(state -> LuaValue.valueOf(state.getBarrierAmount()))
                    .orElse(LuaValue.valueOf(0.0));
        }
    }

    /**
     * sanctuary.setBarrier(entityTable, amount) - 보호막 수치 설정
     */
    private class SetBarrierFunction extends TwoArgFunction {
        @Override
        public LuaValue call(LuaValue entityArg, LuaValue amountArg) {
            SanctuaryEntity entity = getEntityFromArg(entityArg);
            if (entity == null)
                return LuaValue.FALSE;

            StateComponent state = getOrCreateStateComponent(entity);
            state.setBarrierAmount(amountArg.todouble());
            return LuaValue.TRUE;
        }
    }

    /**
     * sanctuary.addBarrier(entityTable, amount) - 보호막 수치 추가
     */
    private class AddBarrierFunction extends TwoArgFunction {
        @Override
        public LuaValue call(LuaValue entityArg, LuaValue amountArg) {
            SanctuaryEntity entity = getEntityFromArg(entityArg);
            if (entity == null)
                return LuaValue.FALSE;

            StateComponent state = getOrCreateStateComponent(entity);
            state.addBarrier(amountArg.todouble());
            return LuaValue.valueOf(state.getBarrierAmount());
        }
    }

    /**
     * sanctuary.hasBarrier(entityTable) - 보호막 존재 여부 확인
     */
    private class HasBarrierFunction extends OneArgFunction {
        @Override
        public LuaValue call(LuaValue entityArg) {
            SanctuaryEntity entity = getEntityFromArg(entityArg);
            if (entity == null)
                return LuaValue.FALSE;

            return entity.getComponentOptional(StateComponent.class)
                    .map(state -> LuaValue.valueOf(state.hasBarrier()))
                    .orElse(LuaValue.FALSE);
        }
    }

    // ===== 파티클 함수 =====

    /**
     * sanctuary.spawnParticle(entityTable, particleName, count) - 파티클 생성
     */
    private class SpawnParticleFunction extends org.luaj.vm2.lib.ThreeArgFunction {
        @Override
        public LuaValue call(LuaValue entityArg, LuaValue particleArg, LuaValue countArg) {
            SanctuaryEntity entity = getEntityFromArg(entityArg);
            if (entity == null || entity.getBukkitEntity() == null)
                return LuaValue.FALSE;

            try {
                org.bukkit.Particle particle = org.bukkit.Particle.valueOf(particleArg.tojstring());
                int count = countArg.isnil() ? 10 : countArg.toint();

                org.bukkit.Location loc = entity.getBukkitEntity().getLocation();
                loc.getWorld().spawnParticle(particle, loc, count, 0.5, 0.5, 0.5, 0.1);
                return LuaValue.TRUE;
            } catch (IllegalArgumentException e) {
                logger.warning("[Lua] spawnParticle: 알 수 없는 파티클: " + particleArg.tojstring());
                return LuaValue.FALSE;
            }
        }
    }

    // ===== 헬퍼 메서드 =====

    /**
     * Lua 테이블에서 SanctuaryEntity를 가져옵니다.
     */
    private SanctuaryEntity getEntityFromArg(LuaValue entityArg) {
        if (!entityArg.istable() || entityManager == null) {
            return null;
        }

        String uuidStr = entityArg.get("uuid").tojstring();
        if (uuidStr == null)
            return null;

        try {
            java.util.UUID uuid = java.util.UUID.fromString(uuidStr);
            return entityManager.get(uuid);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    /**
     * 엔티티에서 StateComponent를 가져오거나 생성합니다.
     */
    private StateComponent getOrCreateStateComponent(SanctuaryEntity entity) {
        StateComponent state = entity.getComponent(StateComponent.class);
        if (state == null) {
            state = new StateComponent();
            entity.attach(state);
        }
        return state;
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
