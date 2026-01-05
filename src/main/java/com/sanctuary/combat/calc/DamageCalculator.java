package com.sanctuary.combat.calc;

import com.sanctuary.combat.event.CombatContext;
import com.sanctuary.combat.event.CombatEventBus;
import com.sanctuary.combat.event.DamageDealtEvent;
import com.sanctuary.combat.model.DamageContext;
import com.sanctuary.combat.stat.Stat;
import com.sanctuary.combat.stat.AttributeContainer;
import com.sanctuary.core.script.ScriptEngine;

import org.luaj.vm2.LuaTable;
import org.luaj.vm2.LuaValue;

import java.util.concurrent.ThreadLocalRandom;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * DamageCalculator (데미지 계산기)
 * 디아블로 4 시즌 6 공식 적용
 * 
 * Lua 스크립트와 통합되어 실제 계산은 Lua에서 수행됩니다.
 * Lua 오류 시 Java 폴백 로직을 사용합니다.
 *
 * 공식: FinalDamage = WeaponDamage * (1 + AdditiveTotal) * Multipliers * Crit *
 * Vuln
 * + Overpower Damage (if proc)
 */
public class DamageCalculator {

    private static final String DAMAGE_SCRIPT = "damage_calculator.lua";
    private static final String CALCULATE_FUNCTION = "calculateFinalDamage";

    private final ScriptEngine scriptEngine;
    private final CombatEventBus eventBus;
    private final Logger logger;

    private boolean luaEnabled = true;

    /**
     * 레거시 생성자 (Lua 없이 Java 전용)
     */
    public DamageCalculator() {
        this.scriptEngine = null;
        this.eventBus = null;
        this.logger = Logger.getLogger(DamageCalculator.class.getName());
        this.luaEnabled = false;
    }

    /**
     * 새 생성자 (Lua와 이벤트 버스 통합)
     */
    public DamageCalculator(ScriptEngine scriptEngine, CombatEventBus eventBus) {
        this.scriptEngine = scriptEngine;
        this.eventBus = eventBus;
        this.logger = Logger.getLogger(DamageCalculator.class.getName());
        this.luaEnabled = scriptEngine != null;
    }

    /**
     * 데미지를 계산합니다.
     * Lua 스크립트가 로드되어 있으면 Lua로 계산하고, 그렇지 않으면 Java 폴백을 사용합니다.
     */
    public double calculate(DamageContext ctx) {
        double result;
        boolean usedLua = false;

        // Lua 계산 시도
        if (luaEnabled && scriptEngine != null) {
            try {
                result = calculateWithLua(ctx);
                usedLua = true;
            } catch (Exception e) {
                logger.log(Level.WARNING, "[DamageCalculator] Lua 계산 실패, Java 폴백 사용: " + e.getMessage());
                result = calculateWithJava(ctx);
            }
        } else {
            result = calculateWithJava(ctx);
        }

        // 이벤트 발생
        if (eventBus != null) {
            fireDamageEvent(ctx, result, usedLua);
        }

        return result;
    }

    /**
     * Lua 스크립트를 사용하여 데미지를 계산합니다.
     */
    private double calculateWithLua(DamageContext ctx) {
        // 스크립트 로드 확인
        scriptEngine.loadScript(DAMAGE_SCRIPT);

        // 컨텍스트를 Lua 테이블로 변환
        LuaTable casterTable = createCasterTable(ctx);
        LuaTable targetTable = createTargetTable(ctx);
        LuaTable skillData = createSkillDataTable(ctx);

        // Lua 함수 호출
        LuaValue result = scriptEngine.callFunction(CALCULATE_FUNCTION, casterTable, targetTable, skillData);

        if (result.istable()) {
            LuaTable resultTable = result.checktable();
            double damage = resultTable.get("damage").todouble();

            // 결과에서 추가 정보 추출
            if (!resultTable.get("isCrit").isnil()) {
                ctx.setCritical(resultTable.get("isCrit").toboolean());
            }
            if (!resultTable.get("isOverpower").isnil()) {
                ctx.setOverpower(resultTable.get("isOverpower").toboolean());
            }

            return damage;
        } else if (result.isnumber()) {
            return result.todouble();
        }

        throw new RuntimeException("Lua 계산 결과가 예상과 다름: " + result);
    }

    /**
     * 공격자 정보를 Lua 테이블로 변환합니다.
     */
    private LuaTable createCasterTable(DamageContext ctx) {
        LuaTable table = new LuaTable();
        AttributeContainer stats = ctx.getAttackerStats();

        // 주요 스탯 설정
        table.set("uuid", ctx.getAttacker().getUniqueId().toString());
        table.set("WEAPON_DAMAGE", stats.getValue(Stat.WEAPON_DAMAGE));
        table.set("STRENGTH", stats.getValue(Stat.STRENGTH));
        table.set("CRIT_CHANCE", stats.getValue(Stat.CRIT_CHANCE));
        table.set("CRIT_DAMAGE", stats.getValue(Stat.CRIT_DAMAGE));
        table.set("OVERPOWER_DAMAGE", stats.getValue(Stat.OVERPOWER_DAMAGE));
        table.set("MAX_HP", stats.getValue(Stat.MAX_HP));

        // 조건부 피해
        table.set("DAMAGE_TO_CLOSE", stats.getValue(Stat.DAMAGE_VS_CLOSE));
        table.set("DAMAGE_TO_DISTANT", stats.getValue(Stat.DAMAGE_VS_DISTANT));
        table.set("DAMAGE_TO_CC", stats.getValue(Stat.DAMAGE_VS_CC));
        table.set("VULNERABLE_DAMAGE", stats.getValue(Stat.VULNERABLE_DAMAGE));

        return table;
    }

    /**
     * 피격자 정보를 Lua 테이블로 변환합니다.
     */
    private LuaTable createTargetTable(DamageContext ctx) {
        LuaTable table = new LuaTable();

        table.set("uuid", ctx.getVictim().getUniqueId().toString());
        table.set("isVulnerable", LuaValue.valueOf(ctx.isVulnerable()));

        // 태그 정보
        LuaTable tags = new LuaTable();
        int i = 1;
        for (String tag : ctx.getTags()) {
            tags.set(i++, tag);
        }
        table.set("tags", tags);

        return table;
    }

    /**
     * 스킬 데이터를 Lua 테이블로 변환합니다.
     */
    private LuaTable createSkillDataTable(DamageContext ctx) {
        LuaTable table = new LuaTable();

        table.set("baseDamage", ctx.getSkillCoefficient());
        table.set("distanceToTarget", 5.0); // TODO: 실제 거리 계산

        return table;
    }

    /**
     * 피해 이벤트를 발생시킵니다.
     */
    private void fireDamageEvent(DamageContext ctx, double damage, boolean usedLua) {
        CombatContext combatContext = CombatContext.builder()
                .skillCoefficient(ctx.getSkillCoefficient())
                .build();

        // 태그 복사
        for (String tag : ctx.getTags()) {
            combatContext.addTag(tag);
        }

        DamageDealtEvent event = new DamageDealtEvent(combatContext, damage)
                .finalDamage(damage)
                .critical(ctx.isCritical())
                .overpower(ctx.isOverpower())
                .vulnerable(ctx.isVulnerable());

        eventBus.fire(event);
    }

    /**
     * Java 폴백 로직으로 데미지를 계산합니다.
     * (기존 로직 유지)
     */
    private double calculateWithJava(DamageContext ctx) {
        AttributeContainer stats = ctx.getAttackerStats();

        // 1. 무기 데미지
        double weaponDamageBase = stats.getValue(Stat.WEAPON_DAMAGE);
        double baseDamage = weaponDamageBase * ctx.getSkillCoefficient();

        // 2. 주 스탯 보너스
        double mainStat = stats.getValue(Stat.STRENGTH);
        double mainStatMultiplier = 1.0 + (mainStat * 0.001);

        // 3. 합연산 버킷
        double additiveBonus = 0.0;
        if (ctx.hasTag("PHYSICAL"))
            additiveBonus += stats.getValue(Stat.PHYSICAL_DAMAGE);
        if (ctx.hasTag("FIRE"))
            additiveBonus += stats.getValue(Stat.FIRE_DAMAGE);
        additiveBonus += stats.getValue(Stat.DAMAGE_VS_CLOSE);
        additiveBonus += stats.getValue(Stat.CRIT_DAMAGE);
        additiveBonus += stats.getValue(Stat.VULNERABLE_DAMAGE);

        double additiveMultiplier = 1.0 + additiveBonus;

        // 4. 곱연산 그룹
        double globalMultiplier = stats.getValue(Stat.GLOBAL_DAMAGE_MULTI);
        if (globalMultiplier == 0)
            globalMultiplier = 1.0;

        // 5. 치명타 및 취약
        double critChance = stats.getValue(Stat.CRIT_CHANCE);
        boolean isCrit = ThreadLocalRandom.current().nextDouble() < critChance;
        ctx.setCritical(isCrit);

        boolean isVuln = ctx.isVulnerable();

        double critMultiplier = isCrit ? 1.5 : 1.0;
        double vulnMultiplier = isVuln ? 1.2 : 1.0;

        double currentDamage = baseDamage * mainStatMultiplier * additiveMultiplier
                * globalMultiplier * critMultiplier * vulnMultiplier;

        // 6. 제압
        boolean isOverpower = ThreadLocalRandom.current().nextDouble() < 0.03;
        if (isOverpower) {
            ctx.setOverpower(true);
            double maxHp = stats.getValue(Stat.MAX_HP);
            double overpowerBonus = stats.getValue(Stat.OVERPOWER_DAMAGE);
            double overpowerDamage = maxHp * 1.5 * (1.0 + overpowerBonus);
            currentDamage += overpowerDamage;
        }

        return currentDamage;
    }

    /**
     * Lua 계산 활성화 여부를 설정합니다.
     */
    public void setLuaEnabled(boolean enabled) {
        this.luaEnabled = enabled && scriptEngine != null;
    }

    /**
     * Lua 계산이 활성화되어 있는지 확인합니다.
     */
    public boolean isLuaEnabled() {
        return luaEnabled;
    }
}
