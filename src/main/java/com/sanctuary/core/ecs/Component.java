package com.sanctuary.core.ecs;

/**
 * 컴포넌트 마커 인터페이스입니다.
 * 모든 ECS 컴포넌트는 이 인터페이스를 구현해야 합니다.
 * 
 * 컴포넌트는 순수한 데이터 컨테이너로,
 * 엔티티에 특정 기능이나 속성을 부여합니다.
 */
public interface Component {

    /**
     * 컴포넌트의 고유 식별자를 반환합니다.
     * 기본적으로 클래스 이름을 사용합니다.
     */
    default String getComponentId() {
        return this.getClass().getSimpleName();
    }
}
