package com.sanctuary.combat.stat;

/**
 * 디아블로 4 스타일의 스탯 정의 (Season 6 기준)
 * stats.json과 동기화됨
 */
public enum Stat {

    // ===== Core Stats (기본 능력치) =====
    /** 힘: 물리 공격력과 방어구 보너스 증가 */
    STRENGTH,
    /** 민첩: 치명타 확률과 회피율 증가 */
    DEXTERITY,
    /** 지능: 마법 공격력과 저항력 증가 */
    INTELLIGENCE,
    /** 의지력: 자원 재생과 치유 효과 증가 */
    WILLPOWER,

    // ===== Offensive (공격) =====
    /** 무기 공격력 (기본 피해량) */
    WEAPON_DAMAGE,
    /** 공격 속도 보너스 (%) */
    ATTACK_SPEED,
    /** 치명타 확률 (기본 5% + @) */
    CRIT_CHANCE,
    /** 치명타 피해 (기본 50% + @, 합연산 적용) */
    CRIT_DAMAGE,
    /** 제압 피해 (합연산) */
    OVERPOWER_DAMAGE,
    /** 취약 피해 (합연산) */
    VULNERABLE_DAMAGE,

    // ===== Additive Bucket (합연산 그룹) =====
    /** 근거리 피해 */
    DAMAGE_VS_CLOSE,
    /** 원거리 피해 */
    DAMAGE_VS_DISTANT,
    /** 정예 상대 피해 */
    DAMAGE_VS_ELITE,
    /** 군중 제어 대상 피해 */
    DAMAGE_VS_CC,
    /** 피해 상태(출혈/독/화상) 대상 피해 */
    DAMAGE_VS_INJURED,
    /** 체력 80% 이상 대상 피해 */
    DAMAGE_VS_HEALTHY,
    /** 물리 피해 */
    PHYSICAL_DAMAGE,
    /** 화염 피해 */
    FIRE_DAMAGE,
    /** 냉기 피해 */
    COLD_DAMAGE,
    /** 번개 피해 */
    LIGHTNING_DAMAGE,
    /** 독 피해 */
    POISON_DAMAGE,
    /** 암흑 피해 */
    SHADOW_DAMAGE,
    /** 출혈 피해 (DoT) */
    BLEED_DAMAGE,

    // ===== Multipliers (곱연산 그룹) =====
    /** 주는 피해 증가 [x] */
    GLOBAL_DAMAGE_MULTI,

    // ===== Defensive (방어) =====
    /** 최대 생명력 */
    MAX_HP,
    /** 방어도 */
    ARMOR,
    /** 회피 확률 */
    DODGE_CHANCE,
    /** 방어막 차단 (방패) */
    BLOCK_CHANCE,
    /** 보호막 최대치 */
    BARRIER_MAX,
    /** 받는 피해 감소 */
    DAMAGE_REDUCTION,

    // ===== 저항력 =====
    /** 화염 저항 */
    RESISTANCE_FIRE,
    /** 냉기 저항 */
    RESISTANCE_COLD,
    /** 번개 저항 */
    RESISTANCE_LIGHTNING,
    /** 독 저항 */
    RESISTANCE_POISON,
    /** 암흑 저항 */
    RESISTANCE_SHADOW,
    /** 모든 저항 (All Resistance) */
    ALL_RESISTANCE,

    // ===== Resource & Utility =====
    /** 최대 자원 */
    MAX_RESOURCE,
    /** 자원 회복량 */
    RESOURCE_REGEN,
    /** 재사용 대기시간 감소 */
    COOLDOWN_REDUCTION,
    /** 행운의 적중 확률 보너스 */
    LUCKY_HIT_CHANCE,
    /** 적중 시 생명력 회복 */
    LIFE_ON_HIT,
    /** 처치 시 생명력 회복 */
    LIFE_ON_KILL,
    /** 이동 속도 */
    MOVEMENT_SPEED,
    /** 치유량 증가 */
    HEALING_RECEIVED,
    /** 포션 회복량 */
    POTION_HEALING,
    /** 가시 피해 (Thorns) */
    THORNS,

    // ===== 행운의 적중 세부 효과 =====
    /** 행운의 적중: 체력 회복 */
    LUCKY_HIT_HEAL,
    /** 행운의 적중: 자원 회복 */
    LUCKY_HIT_RESOURCE
}
