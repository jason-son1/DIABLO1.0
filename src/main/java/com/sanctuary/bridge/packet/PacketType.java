package com.sanctuary.bridge.packet;

/**
 * 커스텀 패킷 타입을 정의합니다.
 * 서버↔클라이언트 통신에 사용되는 패킷 ID입니다.
 */
public enum PacketType {

    // ===== 서버 → 클라이언트 (0x01 ~ 0x7F) =====

    /**
     * 플레이어 스탯 동기화
     */
    S2C_SYNC_STATS(0x01),

    /**
     * 상태 이상 동기화
     */
    S2C_SYNC_STATUS_EFFECTS(0x02),

    /**
     * HUD 전체 렌더링 (SDUI)
     */
    S2C_HUD_RENDER(0x0F),

    /**
     * UI 업데이트 (SDUI)
     */
    S2C_UI_UPDATE(0x10),

    /**
     * 데미지 인디케이터 표시
     */
    S2C_DAMAGE_INDICATOR(0x11),

    /**
     * 보스 UI 표시
     */
    S2C_BOSS_BAR(0x12),

    /**
     * 플로팅 텍스트 표시
     */
    S2C_FLOATING_TEXT(0x13),

    /**
     * 아이템 Lore 동기화
     */
    S2C_ITEM_DATA(0x20),

    /**
     * 인벤토리 동기화
     */
    S2C_INVENTORY_SYNC(0x21),

    // ===== 클라이언트 → 서버 (0x80 ~ 0xFF) =====

    /**
     * 클라이언트 모드 연결 확인
     */
    C2S_HANDSHAKE(0x80),

    /**
     * 스킬 사용 요청
     */
    C2S_SKILL_CAST(0x81),

    /**
     * UI 상호작용
     */
    C2S_UI_INTERACTION(0x90),

    /**
     * 인벤토리 액션 요청
     */
    C2S_INVENTORY_ACTION(0xA0);

    private final int id;

    PacketType(int id) {
        this.id = id;
    }

    public int getId() {
        return id;
    }

    public byte getIdByte() {
        return (byte) id;
    }

    /**
     * ID로 PacketType을 조회합니다.
     */
    public static PacketType fromId(int id) {
        for (PacketType type : values()) {
            if (type.id == id)
                return type;
        }
        return null;
    }

    /**
     * 서버→클라이언트 패킷인지 확인합니다.
     */
    public boolean isServerToClient() {
        return id < 0x80;
    }

    /**
     * 클라이언트→서버 패킷인지 확인합니다.
     */
    public boolean isClientToServer() {
        return id >= 0x80;
    }
}
