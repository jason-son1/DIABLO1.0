package com.sanctuary.combat.event;

/**
 * 전투 이벤트 인터페이스
 * 모든 전투 이벤트가 구현해야 하는 기본 계약입니다.
 */
public interface CombatEvent {

    /**
     * 이벤트 타입을 반환합니다.
     */
    CombatEventType getType();

    /**
     * 이벤트와 관련된 전투 컨텍스트를 반환합니다.
     */
    CombatContext getContext();

    /**
     * 이벤트가 취소되었는지 확인합니다.
     */
    boolean isCancelled();

    /**
     * 이벤트를 취소합니다.
     * 취소된 이벤트는 후속 리스너에게 전달되지만, 최종 처리되지 않습니다.
     */
    void setCancelled(boolean cancelled);
}
