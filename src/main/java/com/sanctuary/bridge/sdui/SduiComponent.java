package com.sanctuary.bridge.sdui;

import java.util.*;

/**
 * SDUI 컴포넌트 정의 클래스입니다.
 * JSON으로 직렬화되어 클라이언트에 전송됩니다.
 */
public class SduiComponent {

    private String id; // 고유 ID
    private SduiComponentType type; // 컴포넌트 타입
    private Map<String, Object> props; // 속성
    private List<SduiComponent> children; // 자식 컴포넌트
    private Map<String, String> eventHandlers; // 이벤트 핸들러 ID

    public SduiComponent(String id, SduiComponentType type) {
        this.id = id;
        this.type = type;
        this.props = new HashMap<>();
        this.children = new ArrayList<>();
        this.eventHandlers = new HashMap<>();
    }

    // ===== 빌더 패턴 =====

    public SduiComponent prop(String key, Object value) {
        props.put(key, value);
        return this;
    }

    public SduiComponent child(SduiComponent child) {
        children.add(child);
        return this;
    }

    public SduiComponent children(SduiComponent... components) {
        children.addAll(Arrays.asList(components));
        return this;
    }

    public SduiComponent onClick(String handlerId) {
        eventHandlers.put("onClick", handlerId);
        return this;
    }

    public SduiComponent onHover(String handlerId) {
        eventHandlers.put("onHover", handlerId);
        return this;
    }

    // ===== 팩토리 메서드 =====

    /**
     * 텍스트 컴포넌트를 생성합니다.
     */
    public static SduiComponent text(String id, String content) {
        return new SduiComponent(id, SduiComponentType.TEXT)
                .prop("text", content);
    }

    /**
     * 색상 텍스트 컴포넌트를 생성합니다.
     */
    public static SduiComponent colorText(String id, String content, String color) {
        return text(id, content).prop("color", color);
    }

    /**
     * 진행 바를 생성합니다.
     */
    public static SduiComponent progressBar(String id, double value, double max, String color) {
        return new SduiComponent(id, SduiComponentType.PROGRESS_BAR)
                .prop("value", value)
                .prop("max", max)
                .prop("color", color);
    }

    /**
     * 체력 오브를 생성합니다.
     */
    public static SduiComponent healthOrb(String id, double current, double max) {
        return new SduiComponent(id, SduiComponentType.HEALTH_ORB)
                .prop("current", current)
                .prop("max", max);
    }

    /**
     * 자원 오브를 생성합니다.
     */
    public static SduiComponent resourceOrb(String id, double current, double max, String resourceType) {
        return new SduiComponent(id, SduiComponentType.RESOURCE_ORB)
                .prop("current", current)
                .prop("max", max)
                .prop("resourceType", resourceType);
    }

    /**
     * 스킬 버튼을 생성합니다.
     */
    public static SduiComponent skillButton(String id, String skillId, String icon, double cooldown) {
        return new SduiComponent(id, SduiComponentType.BUTTON)
                .prop("skillId", skillId)
                .prop("icon", icon)
                .prop("cooldown", cooldown);
    }

    /**
     * 보스 체력바를 생성합니다.
     */
    public static SduiComponent bossBar(String id, String bossName, double hp, double maxHp,
            double stagger, double maxStagger) {
        return new SduiComponent(id, SduiComponentType.BOSS_BAR)
                .prop("name", bossName)
                .prop("hp", hp)
                .prop("maxHp", maxHp)
                .prop("stagger", stagger)
                .prop("maxStagger", maxStagger);
    }

    /**
     * 데미지 텍스트를 생성합니다.
     */
    public static SduiComponent damageText(String id, double damage, boolean crit, boolean overpower,
            double x, double y, double z) {
        return new SduiComponent(id, SduiComponentType.DAMAGE_TEXT)
                .prop("damage", damage)
                .prop("crit", crit)
                .prop("overpower", overpower)
                .prop("x", x)
                .prop("y", y)
                .prop("z", z);
    }

    // ===== Getters =====

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public SduiComponentType getType() {
        return type;
    }

    public Map<String, Object> getProps() {
        return props;
    }

    public List<SduiComponent> getChildren() {
        return children;
    }

    public Map<String, String> getEventHandlers() {
        return eventHandlers;
    }

    /**
     * Map으로 변환합니다 (JSON 직렬화용).
     */
    public Map<String, Object> toMap() {
        Map<String, Object> map = new HashMap<>();
        map.put("id", id);
        map.put("type", type.name());
        map.put("props", props);

        if (!children.isEmpty()) {
            List<Map<String, Object>> childMaps = new ArrayList<>();
            for (SduiComponent child : children) {
                childMaps.add(child.toMap());
            }
            map.put("children", childMaps);
        }

        if (!eventHandlers.isEmpty()) {
            map.put("events", eventHandlers);
        }

        return map;
    }
}
