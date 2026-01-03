package com.sanctuary.bridge.sdui;

/**
 * SDUI(Server-Driven UI) 컴포넌트 타입을 정의합니다.
 */
public enum SduiComponentType {

    // ===== 레이아웃 =====
    CONTAINER, // 컨테이너 (자식 요소 포함)
    HORIZONTAL, // 가로 배열
    VERTICAL, // 세로 배열
    OVERLAY, // 오버레이

    // ===== 표시 요소 =====
    TEXT, // 텍스트
    ICON, // 아이콘
    IMAGE, // 이미지
    PROGRESS_BAR, // 진행 바

    // ===== 상호작용 =====
    BUTTON, // 버튼
    TOGGLE, // 토글
    SLOT, // 아이템 슬롯

    // ===== 커스텀 =====
    HEALTH_ORB, // 체력 오브 (디아블로 스타일)
    RESOURCE_ORB, // 자원 오브
    SKILL_BAR, // 스킬 바
    MINIMAP, // 미니맵
    BOSS_BAR, // 보스 체력바
    DAMAGE_TEXT // 데미지 텍스트
}
