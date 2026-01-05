package com.sanctuary.combat.event;

/**
 * 전투 이벤트 리스너 인터페이스
 * Java 코드에서 전투 이벤트를 수신하기 위한 콜백 인터페이스입니다.
 */
@FunctionalInterface
public interface CombatEventListener {

    /**
     * 전투 이벤트가 발생했을 때 호출됩니다.
     * 
     * @param event 발생한 전투 이벤트
     */
    void onEvent(CombatEvent event);
}
