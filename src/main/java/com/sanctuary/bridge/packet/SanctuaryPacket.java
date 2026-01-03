package com.sanctuary.bridge.packet;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

/**
 * 커스텀 패킷 데이터를 담는 클래스입니다.
 * 플러그인 채널을 통해 전송됩니다.
 */
public class SanctuaryPacket {

    private static final Gson GSON = new GsonBuilder().create();

    private final PacketType type;
    private final Map<String, Object> data;
    private final long timestamp;

    public SanctuaryPacket(PacketType type) {
        this.type = type;
        this.data = new HashMap<>();
        this.timestamp = System.currentTimeMillis();
    }

    public SanctuaryPacket(PacketType type, Map<String, Object> data) {
        this.type = type;
        this.data = data != null ? data : new HashMap<>();
        this.timestamp = System.currentTimeMillis();
    }

    // ===== 데이터 설정 =====

    public SanctuaryPacket put(String key, Object value) {
        data.put(key, value);
        return this;
    }

    public SanctuaryPacket putAll(Map<String, Object> values) {
        data.putAll(values);
        return this;
    }

    // ===== 데이터 조회 =====

    @SuppressWarnings("unchecked")
    public <T> T get(String key) {
        return (T) data.get(key);
    }

    public <T> T get(String key, T defaultValue) {
        Object value = data.get(key);
        if (value == null)
            return defaultValue;
        return (T) value;
    }

    public String getString(String key) {
        Object value = data.get(key);
        return value != null ? value.toString() : null;
    }

    public int getInt(String key) {
        Object value = data.get(key);
        if (value instanceof Number)
            return ((Number) value).intValue();
        return 0;
    }

    public double getDouble(String key) {
        Object value = data.get(key);
        if (value instanceof Number)
            return ((Number) value).doubleValue();
        return 0.0;
    }

    public boolean getBoolean(String key) {
        Object value = data.get(key);
        if (value instanceof Boolean)
            return (Boolean) value;
        return false;
    }

    // ===== 직렬화 =====

    /**
     * 패킷을 바이트 배열로 직렬화합니다.
     * 포맷: [1바이트 타입][4바이트 길이][JSON 페이로드]
     */
    public byte[] serialize() {
        String json = GSON.toJson(data);
        byte[] jsonBytes = json.getBytes(StandardCharsets.UTF_8);

        ByteBuffer buffer = ByteBuffer.allocate(1 + 4 + jsonBytes.length);
        buffer.put(type.getIdByte());
        buffer.putInt(jsonBytes.length);
        buffer.put(jsonBytes);

        return buffer.array();
    }

    /**
     * 바이트 배열에서 패킷을 역직렬화합니다.
     */
    public static SanctuaryPacket deserialize(byte[] bytes) {
        ByteBuffer buffer = ByteBuffer.wrap(bytes);

        int typeId = buffer.get() & 0xFF;
        PacketType type = PacketType.fromId(typeId);
        if (type == null)
            return null;

        int length = buffer.getInt();
        byte[] jsonBytes = new byte[length];
        buffer.get(jsonBytes);

        String json = new String(jsonBytes, StandardCharsets.UTF_8);
        @SuppressWarnings("unchecked")
        Map<String, Object> data = GSON.fromJson(json, Map.class);

        return new SanctuaryPacket(type, data);
    }

    // ===== Getters =====

    public PacketType getType() {
        return type;
    }

    public Map<String, Object> getData() {
        return data;
    }

    public long getTimestamp() {
        return timestamp;
    }

    @Override
    public String toString() {
        return "SanctuaryPacket{type=" + type + ", data=" + data + "}";
    }

    // ===== 팩토리 메서드 =====

    /**
     * 스탯 동기화 패킷을 생성합니다.
     */
    public static SanctuaryPacket syncStats(Map<String, Double> stats) {
        return new SanctuaryPacket(PacketType.S2C_SYNC_STATS)
                .put("stats", stats);
    }

    /**
     * 데미지 인디케이터 패킷을 생성합니다.
     */
    public static SanctuaryPacket damageIndicator(double damage, boolean crit, boolean overpower,
            double x, double y, double z) {
        return new SanctuaryPacket(PacketType.S2C_DAMAGE_INDICATOR)
                .put("damage", damage)
                .put("crit", crit)
                .put("overpower", overpower)
                .put("x", x)
                .put("y", y)
                .put("z", z);
    }

    /**
     * UI 업데이트 패킷을 생성합니다.
     */
    public static SanctuaryPacket uiUpdate(String componentId, Map<String, Object> props) {
        return new SanctuaryPacket(PacketType.S2C_UI_UPDATE)
                .put("componentId", componentId)
                .put("props", props);
    }
}
