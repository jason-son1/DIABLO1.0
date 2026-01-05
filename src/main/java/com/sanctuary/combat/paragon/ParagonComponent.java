package com.sanctuary.combat.paragon;

import com.sanctuary.core.ecs.Component;

import java.util.*;

/**
 * 플레이어 정복자 보드 상태 Component
 * 활성화된 노드, 장착된 문양 등을 관리합니다.
 */
public class ParagonComponent implements Component {

    // 활성화된 노드 (보드ID -> 노드ID 목록)
    private final Map<String, Set<String>> activatedNodes = new HashMap<>();

    // 장착된 문양 (소켓 노드ID -> 문양ID)
    private final Map<String, String> glyphSlots = new HashMap<>();

    // 정복자 포인트
    private int paragonPoints = 0;
    private int usedPoints = 0;

    // 현재 활성 보드 ID
    private String activeBoardId = "starter_board";

    /**
     * 노드를 활성화합니다.
     */
    public boolean activateNode(String boardId, String nodeId, int cost) {
        if (paragonPoints - usedPoints < cost) {
            return false;
        }

        activatedNodes.computeIfAbsent(boardId, k -> new HashSet<>()).add(nodeId);
        usedPoints += cost;
        return true;
    }

    /**
     * 노드가 활성화되었는지 확인합니다.
     */
    public boolean isNodeActivated(String boardId, String nodeId) {
        Set<String> nodes = activatedNodes.get(boardId);
        return nodes != null && nodes.contains(nodeId);
    }

    /**
     * 보드의 활성화된 노드 목록을 반환합니다.
     */
    public Set<String> getActivatedNodes(String boardId) {
        return activatedNodes.getOrDefault(boardId, Collections.emptySet());
    }

    /**
     * 문양을 장착합니다.
     */
    public void equipGlyph(String socketNodeId, String glyphId) {
        glyphSlots.put(socketNodeId, glyphId);
    }

    /**
     * 문양을 해제합니다.
     */
    public String unequipGlyph(String socketNodeId) {
        return glyphSlots.remove(socketNodeId);
    }

    /**
     * 장착된 문양을 반환합니다.
     */
    public String getEquippedGlyph(String socketNodeId) {
        return glyphSlots.get(socketNodeId);
    }

    /**
     * 정복자 포인트를 추가합니다. (레벨 50+ 경험치)
     */
    public void addParagonPoints(int points) {
        paragonPoints += points;
    }

    /**
     * 사용 가능한 포인트를 반환합니다.
     */
    public int getAvailablePoints() {
        return paragonPoints - usedPoints;
    }

    /**
     * 총 정복자 포인트를 반환합니다.
     */
    public int getTotalParagonPoints() {
        return paragonPoints;
    }

    /**
     * 사용된 포인트를 반환합니다.
     */
    public int getUsedPoints() {
        return usedPoints;
    }

    /**
     * 활성 보드를 변경합니다.
     */
    public void setActiveBoard(String boardId) {
        this.activeBoardId = boardId;
    }

    /**
     * 활성 보드 ID를 반환합니다.
     */
    public String getActiveBoardId() {
        return activeBoardId;
    }

    /**
     * 보드를 리스펙합니다.
     */
    public void respecBoard(String boardId) {
        Set<String> nodes = activatedNodes.remove(boardId);
        if (nodes != null) {
            // 사용된 포인트 환불 (실제로는 노드별 비용 계산 필요)
            // 여기서는 단순화
        }
    }

    /**
     * 전체 리스펙합니다.
     */
    public void respecAll() {
        activatedNodes.clear();
        glyphSlots.clear();
        usedPoints = 0;
    }

    /**
     * 전체 활성화된 노드 수를 반환합니다.
     */
    public int getTotalActivatedNodes() {
        return activatedNodes.values().stream()
                .mapToInt(Set::size)
                .sum();
    }
}
