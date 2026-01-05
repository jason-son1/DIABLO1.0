package com.sanctuary.combat.paragon;

/**
 * 정복자 보드 노드
 * 각 노드는 활성화 시 스탯이나 효과를 부여합니다.
 */
public class ParagonNode {

    // 노드 타입
    public enum NodeType {
        NORMAL("일반", "§f", 1), // 기본 스탯 노드
        MAGIC("마법", "§9", 5), // 마법 속성 노드
        RARE("희귀", "§e", 10), // 희귀 효과 노드
        GLYPH_SOCKET("문양 소켓", "§d", 0), // 문양 장착 슬롯
        LEGENDARY("전설", "§6", 20); // 강력한 패시브 효과

        public final String name;
        public final String color;
        public final int pointCost;

        NodeType(String name, String color, int pointCost) {
            this.name = name;
            this.color = color;
            this.pointCost = pointCost;
        }
    }

    private final String id;
    private final NodeType type;
    private final String stat; // 적용되는 스탯 (예: "STRENGTH", "CRIT_CHANCE")
    private final double value; // 스탯 증가량
    private final String effect; // 특수 효과 ID (RARE/LEGENDARY 노드용)

    // 그리드 위치
    private final int gridX;
    private final int gridY;

    public ParagonNode(String id, NodeType type, String stat, double value, int gridX, int gridY) {
        this(id, type, stat, value, null, gridX, gridY);
    }

    public ParagonNode(String id, NodeType type, String stat, double value, String effect, int gridX, int gridY) {
        this.id = id;
        this.type = type;
        this.stat = stat;
        this.value = value;
        this.effect = effect;
        this.gridX = gridX;
        this.gridY = gridY;
    }

    public String getId() {
        return id;
    }

    public NodeType getType() {
        return type;
    }

    public String getStat() {
        return stat;
    }

    public double getValue() {
        return value;
    }

    public String getEffect() {
        return effect;
    }

    public int getGridX() {
        return gridX;
    }

    public int getGridY() {
        return gridY;
    }

    public int getPointCost() {
        return type.pointCost;
    }

    public boolean hasEffect() {
        return effect != null && !effect.isEmpty();
    }

    /**
     * 노드 표시 문자열
     */
    public String getDisplayName() {
        return type.color + "[" + type.name + "] " + stat + " +" + formatValue();
    }

    private String formatValue() {
        // 퍼센트 스탯인 경우
        if (stat.contains("CHANCE") || stat.contains("PERCENT") || stat.contains("REDUCTION")) {
            return String.format("%.1f%%", value * 100);
        }
        // 정수 스탯인 경우
        return String.format("%.0f", value);
    }

    @Override
    public String toString() {
        return String.format("ParagonNode{id='%s', type=%s, stat='%s', value=%.2f, pos=(%d,%d)}",
                id, type, stat, value, gridX, gridY);
    }
}
