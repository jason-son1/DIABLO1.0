package com.sanctuary.combat.status;

/**
 * 상태 이상의 타입을 정의합니다.
 */
public enum StatusType {
    /**
     * 버프 - 대상에게 이로운 효과
     */
    BUFF,

    /**
     * 디버프 - 대상에게 해로운 효과
     */
    DEBUFF,

    /**
     * 군중 제어 - 이동/행동을 제한하는 효과
     */
    CROWD_CONTROL
}
