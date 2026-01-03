package com.sanctuary.combat.stat;

/**
 * 디아블로 4 스타일의 스탯 정의 (Season 6 기준)
 */
public enum Stat {

    // --- Core Stats (기본 능력치) ---
    STRENGTH, // 힘: 방어도 + 스킬 데미지 (일부 직업)
    DEXTERITY, // 민첩: 회피 확률 + 치명타 확률 (일부 직업)
    INTELLIGENCE, // 지능: 모든 저항 + 자원 회복 (일부 직업)
    WILLPOWER, // 의지력: 치유량 + 제압 피해

    // --- Offensive (공격) ---
    WEAPON_DAMAGE, // 무기 공격력 (Min-Max 사이의 랜덤값으로 결정됨)
    ATTACK_SPEED, // 공격 속도 보너스 (%)
    CRIT_CHANCE, // 치명타 확률 (기본 5% + @)
    CRIT_DAMAGE, // 치명타 피해 (기본 50% + @, 합연산으로 적용)

    OVERPOWER_DAMAGE, // 제압 피해 (합연산)
    VULNERABLE_DAMAGE, // 취약 피해 (합연산)

    // --- Additive Bucket (합연산 그룹) ---
    DAMAGE_VS_CLOSE, // 근거리 피해
    DAMAGE_VS_DISTANT, // 원거리 피해
    DAMAGE_VS_ELITE, // 정예 상대 피해
    DAMAGE_VS_CC, // 군중 제어 대상 피해
    PHYSICAL_DAMAGE, // 물리 피해
    FIRE_DAMAGE, // 화염 피해
    COLD_DAMAGE, // 냉기 피해
    LIGHTNING_DAMAGE, // 번개 피해
    POISON_DAMAGE, // 독 피해
    SHADOW_DAMAGE, // 암흑 피해

    // --- Multipliers (곱연산 그룹 - 전설 위상 등에서 사용) ---
    // 이 부분은 Stat Enum보다는 별도의 Modifier 객체로 관리되지만, 기본 스탯으로 존재하는 것들만 정의
    GLOBAL_DAMAGE_MULTI, // 주는 피해 증가 [x]

    // --- Defensive (방어) ---
    MAX_HP, // 최대 생명력
    ARMOR, // 방어도
    DODGE_CHANCE, // 회피 확률
    BARRIER_MAX, // 보호막 최대치

    RESISTANCE_FIRE,
    RESISTANCE_COLD,
    RESISTANCE_LIGHTNING,
    RESISTANCE_POISON,
    RESISTANCE_SHADOW,

    // --- Resource & Utility ---
    MAX_RESOURCE, // 최대 자원
    RESOURCE_REGEN, // 자원 회복량
    COOLDOWN_REDUCTION, // 재사용 대기시간 감소
    LUCKY_HIT_CHANCE, // 행운의 적중 확률 보너스
    MOVEMENT_SPEED // 이동 속도
}
