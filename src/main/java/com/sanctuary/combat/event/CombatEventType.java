package com.sanctuary.combat.event;

/**
 * 전투 이벤트 타입 열거형
 * 디아블로 4의 전투 시스템에서 발생하는 모든 이벤트 종류를 정의합니다.
 */
public enum CombatEventType {

    // ===== 피해 관련 =====
    /** 피해를 입힘 (공격자 관점) */
    DAMAGE_DEALT,

    /** 피해를 받음 (피격자 관점) */
    DAMAGE_RECEIVED,

    /** 치명타 발동 */
    CRITICAL_HIT,

    /** 행운의 적중 발동 */
    LUCKY_HIT,

    /** 제압(Overpower) 발동 */
    OVERPOWER,

    // ===== 상태 이상 =====
    /** 취약 상태 적용 */
    VULNERABLE_APPLIED,

    /** 상태 이상 적용 (범용) */
    STATUS_APPLIED,

    /** 상태 이상 해제 */
    STATUS_REMOVED,

    // ===== 스킬/자원 =====
    /** 스킬 시전 */
    SKILL_CAST,

    /** 스킬 적중 */
    SKILL_HIT,

    /** 자원 소모 (마나, 분노 등) */
    RESOURCE_SPENT,

    /** 자원 획득 */
    RESOURCE_GAINED,

    // ===== 생존/사망 =====
    /** 보호막 적용/갱신 */
    BARRIER_APPLIED,

    /** 보강 적용/갱신 */
    FORTIFY_APPLIED,

    /** 처치 (공격자 관점) */
    KILL,

    /** 사망 (피격자 관점) */
    DEATH,

    /** 체력 회복 */
    HEAL
}
