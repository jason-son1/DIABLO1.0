package com.sanctuary.bridge.sdui;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Server-Driven UI 컴포넌트를 정의합니다.
 * 서버에서 UI 레이아웃을 정의하고 클라이언트에 전송합니다.
 */
public class UIComponent {

    private final String type; // 컴포넌트 타입 (TEXT, BAR, ICON, CONTAINER 등)
    private final String id; // 고유 식별자
    private final Map<String, Object> properties; // 속성 맵
    private final List<UIComponent> children; // 자식 컴포넌트

    private UIComponent(String type, String id) {
        this.type = type;
        this.id = id;
        this.properties = new HashMap<>();
        this.children = new ArrayList<>();
    }

    // ===== 팩토리 메서드 =====

    /**
     * 텍스트 컴포넌트를 생성합니다.
     */
    public static UIComponent text(String id, String content) {
        UIComponent comp = new UIComponent("TEXT", id);
        comp.properties.put("text", content);
        return comp;
    }

    /**
     * 프로그레스 바 컴포넌트를 생성합니다.
     */
    public static UIComponent progressBar(String id, double value, double max) {
        UIComponent comp = new UIComponent("PROGRESS_BAR", id);
        comp.properties.put("value", value);
        comp.properties.put("max", max);
        return comp;
    }

    /**
     * 체력 바를 생성합니다 (특수 프로그레스 바).
     */
    public static UIComponent healthBar(String id, double current, double max, double barrier) {
        UIComponent comp = new UIComponent("HEALTH_BAR", id);
        comp.properties.put("current", current);
        comp.properties.put("max", max);
        comp.properties.put("barrier", barrier);
        return comp;
    }

    /**
     * 리소스 바 (마나, 분노 등)를 생성합니다.
     */
    public static UIComponent resourceBar(String id, double current, double max, String resourceType) {
        UIComponent comp = new UIComponent("RESOURCE_BAR", id);
        comp.properties.put("current", current);
        comp.properties.put("max", max);
        comp.properties.put("resourceType", resourceType);
        return comp;
    }

    /**
     * 스킬 아이콘을 생성합니다.
     */
    public static UIComponent skillIcon(String id, String skillId, double cooldown, double maxCooldown) {
        UIComponent comp = new UIComponent("SKILL_ICON", id);
        comp.properties.put("skillId", skillId);
        comp.properties.put("cooldown", cooldown);
        comp.properties.put("maxCooldown", maxCooldown);
        return comp;
    }

    /**
     * 상태 아이콘 (버프/디버프)을 생성합니다.
     */
    public static UIComponent statusIcon(String id, String effectId, double duration, int stacks) {
        UIComponent comp = new UIComponent("STATUS_ICON", id);
        comp.properties.put("effectId", effectId);
        comp.properties.put("duration", duration);
        comp.properties.put("stacks", stacks);
        return comp;
    }

    /**
     * 컨테이너 (레이아웃 그룹)를 생성합니다.
     */
    public static UIComponent container(String id, String layout) {
        UIComponent comp = new UIComponent("CONTAINER", id);
        comp.properties.put("layout", layout); // HORIZONTAL, VERTICAL, GRID
        return comp;
    }

    /**
     * 툴팁을 생성합니다.
     */
    public static UIComponent tooltip(String id, String title, List<String> lines) {
        UIComponent comp = new UIComponent("TOOLTIP", id);
        comp.properties.put("title", title);
        comp.properties.put("lines", lines);
        return comp;
    }

    // ===== 빌더 패턴 =====

    public UIComponent prop(String key, Object value) {
        this.properties.put(key, value);
        return this;
    }

    public UIComponent color(String color) {
        return prop("color", color);
    }

    public UIComponent position(int x, int y) {
        return prop("x", x).prop("y", y);
    }

    public UIComponent size(int width, int height) {
        return prop("width", width).prop("height", height);
    }

    public UIComponent visible(boolean visible) {
        return prop("visible", visible);
    }

    public UIComponent addChild(UIComponent child) {
        this.children.add(child);
        return this;
    }

    // ===== Getters =====

    public String getType() {
        return type;
    }

    public String getId() {
        return id;
    }

    public Map<String, Object> getProperties() {
        return properties;
    }

    public List<UIComponent> getChildren() {
        return children;
    }

    /**
     * 직렬화용 맵으로 변환합니다.
     */
    public Map<String, Object> toMap() {
        Map<String, Object> map = new HashMap<>();
        map.put("type", type);
        map.put("id", id);
        map.put("properties", properties);

        if (!children.isEmpty()) {
            List<Map<String, Object>> childMaps = new ArrayList<>();
            for (UIComponent child : children) {
                childMaps.add(child.toMap());
            }
            map.put("children", childMaps);
        }

        return map;
    }
}
