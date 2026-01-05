package com.sanctuary.combat.skill;

import com.sanctuary.core.ecs.EntityManager;
import com.sanctuary.core.ecs.SanctuaryEntity;
import com.sanctuary.core.ecs.component.classmech.ClassMechanic;
import com.sanctuary.core.script.ScriptEngine;
import org.bukkit.entity.Player;
import org.luaj.vm2.LuaTable;
import org.luaj.vm2.LuaValue;

import java.util.logging.Logger;

/**
 * 스킬 실행기
 * 스킬 사용 요청을 처리하고, 조건 검증 후 Lua 스크립트를 호출합니다.
 */
public class SkillExecutor {

    private final Logger logger;
    private final SkillTreeManager skillTreeManager;
    private final EntityManager entityManager;
    private final ScriptEngine scriptEngine;

    public SkillExecutor(Logger logger, SkillTreeManager skillTreeManager,
            EntityManager entityManager, ScriptEngine scriptEngine) {
        this.logger = logger;
        this.skillTreeManager = skillTreeManager;
        this.entityManager = entityManager;
        this.scriptEngine = scriptEngine;
    }

    /**
     * 스킬 사용을 시도합니다.
     */
    public SkillResult useSkill(Player player, String skillId) {
        SanctuaryEntity entity = entityManager.get(player.getUniqueId());
        if (entity == null) {
            return SkillResult.FAIL_NO_ENTITY;
        }

        SkillData skill = skillTreeManager.getSkillData(skillId);
        if (skill == null) {
            return SkillResult.FAIL_UNKNOWN_SKILL;
        }

        SkillComponent skillComp = entity.getComponent(SkillComponent.class);
        if (skillComp == null) {
            return SkillResult.FAIL_NO_COMPONENT;
        }

        // 1. 스킬 해금 확인
        if (!skillComp.isSkillUnlocked(skillId)) {
            return SkillResult.FAIL_NOT_UNLOCKED;
        }

        // 2. 쿨타임 확인
        if (skillComp.isOnCooldown(skillId)) {
            return SkillResult.FAIL_COOLDOWN;
        }

        // 3. 자원 확인 및 소모
        ClassMechanic mechanic = entity.getComponent(ClassMechanic.class);
        if (mechanic != null && skill.getResourceCost() > 0) {
            if (!mechanic.consumeResource(skill.getResourceCost())) {
                return SkillResult.FAIL_NO_RESOURCE;
            }
        }

        // 4. 자원 생성
        if (mechanic != null && skill.getResourceGenerate() > 0) {
            mechanic.generateResource(skill.getResourceGenerate());
        }

        // 5. 쿨타임 시작
        if (skill.getCooldown() > 0) {
            skillComp.startCooldown(skillId, skill.getCooldown());
        }

        // 6. 직업 메커니즘 훅 호출
        if (mechanic != null) {
            mechanic.onSkillUse(skillId, skill.getCategory());
        }

        // 7. Lua 스크립트 실행
        executeSkillScript(player, entity, skill, skillComp.getSkillRank(skillId));

        logger.fine("[SkillExecutor] 스킬 사용: " + skillId + " by " + player.getName());
        return SkillResult.SUCCESS;
    }

    /**
     * Lua 스킬 스크립트를 실행합니다.
     */
    private void executeSkillScript(Player player, SanctuaryEntity entity,
            SkillData skill, int rank) {
        if (scriptEngine == null) {
            return;
        }

        try {
            LuaTable context = new LuaTable();

            // 스킬 정보
            context.set("skillId", skill.getId());
            context.set("skillName", skill.getName());
            context.set("category", skill.getCategory());
            context.set("damageType", skill.getDamageType());
            context.set("baseDamage", skill.getDamageAtRank(rank));
            context.set("rank", rank);
            context.set("luckyHitChance", skill.getLuckyHitChance());
            context.set("radius", skill.getRadius());
            context.set("isRanged", LuaValue.valueOf(skill.isRanged()));

            // 플레이어 정보
            LuaTable playerTable = new LuaTable();
            playerTable.set("uuid", player.getUniqueId().toString());
            playerTable.set("name", player.getName());
            playerTable.set("x", player.getLocation().getX());
            playerTable.set("y", player.getLocation().getY());
            playerTable.set("z", player.getLocation().getZ());
            context.set("player", playerTable);

            // 스킬 실행 함수 호출
            String functionName = "skill_" + skill.getId().toLowerCase();
            scriptEngine.callFunction(functionName, context);

        } catch (Exception e) {
            logger.warning("[SkillExecutor] 스킬 스크립트 오류: " + e.getMessage());
        }
    }

    /**
     * 스킬 쿨타임을 초기화합니다.
     */
    public void resetCooldown(Player player, String skillId) {
        SanctuaryEntity entity = entityManager.get(player.getUniqueId());
        if (entity != null) {
            SkillComponent skillComp = entity.getComponent(SkillComponent.class);
            if (skillComp != null) {
                skillComp.resetCooldown(skillId);
            }
        }
    }

    /**
     * 스킬 사용 결과
     */
    public enum SkillResult {
        SUCCESS,
        FAIL_NO_ENTITY,
        FAIL_UNKNOWN_SKILL,
        FAIL_NO_COMPONENT,
        FAIL_NOT_UNLOCKED,
        FAIL_COOLDOWN,
        FAIL_NO_RESOURCE,
        FAIL_SCRIPT_ERROR
    }

    /**
     * 결과 메시지를 반환합니다.
     */
    public static String getResultMessage(SkillResult result) {
        return switch (result) {
            case SUCCESS -> "§a스킬 사용!";
            case FAIL_NO_ENTITY -> "§c엔티티 정보 없음";
            case FAIL_UNKNOWN_SKILL -> "§c알 수 없는 스킬";
            case FAIL_NO_COMPONENT -> "§c스킬 컴포넌트 없음";
            case FAIL_NOT_UNLOCKED -> "§c스킬이 해금되지 않음";
            case FAIL_COOLDOWN -> "§c쿨타임 중";
            case FAIL_NO_RESOURCE -> "§c자원 부족";
            case FAIL_SCRIPT_ERROR -> "§c스크립트 오류";
        };
    }
}
