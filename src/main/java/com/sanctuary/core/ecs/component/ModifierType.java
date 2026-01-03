package com.sanctuary.core.ecs.component;

/**
 * 수정자(Modifier)의 타입을 정의합니다.
 * 디아블로 IV의 데미지 버킷 시스템에서 사용됩니다.
 */
public enum ModifierType {
    /**
     * 기본값에 더해지는 고정 수치입니다.
     * 예: +50 공격력
     */
    BASE,

    /**
     * 합연산 퍼센트입니다.
     * 모든 합연산 보너스를 더한 후 기본값에 곱합니다.
     * 예: +20% 화염 피해, +15% 근접 피해 → 총 +35%
     */
    ADDITIVE,

    /**
     * 곱연산 퍼센트입니다.
     * 각 곱연산 보너스를 개별적으로 곱합니다.
     * 예: x1.15, x1.20 → 총 x1.38
     */
    MULTIPLICATIVE
}
