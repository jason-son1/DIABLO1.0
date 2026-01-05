package com.sanctuary.combat.paragon;

import java.util.*;

/**
 * 정복자 보드
 * 노드 그리드와 연결 관계를 관리합니다.
 */
public class ParagonBoard {

    private final String id;
    private final String name;
    private final int width;
    private final int height;

    // 그리드 (x, y) -> 노드
    private final Map<String, ParagonNode> nodes = new HashMap<>();

    // 연결 관계 (노드ID -> 인접 노드ID 목록)
    private final Map<String, Set<String>> connections = new HashMap<>();

    // 시작 노드 ID
    private String startNodeId;

    public ParagonBoard(String id, String name, int width, int height) {
        this.id = id;
        this.name = name;
        this.width = width;
        this.height = height;
    }

    /**
     * 노드를 추가합니다.
     */
    public void addNode(ParagonNode node) {
        String key = node.getId();
        nodes.put(key, node);
        connections.putIfAbsent(key, new HashSet<>());
    }

    /**
     * 두 노드를 연결합니다. (양방향)
     */
    public void connect(String nodeId1, String nodeId2) {
        connections.computeIfAbsent(nodeId1, k -> new HashSet<>()).add(nodeId2);
        connections.computeIfAbsent(nodeId2, k -> new HashSet<>()).add(nodeId1);
    }

    /**
     * 노드를 반환합니다.
     */
    public ParagonNode getNode(String nodeId) {
        return nodes.get(nodeId);
    }

    /**
     * 노드의 인접 노드 ID 목록을 반환합니다.
     */
    public Set<String> getAdjacentNodes(String nodeId) {
        return connections.getOrDefault(nodeId, Collections.emptySet());
    }

    /**
     * 시작 노드에서 도달 가능한지 확인합니다.
     */
    public boolean isReachable(String targetNodeId, Set<String> activatedNodes) {
        if (startNodeId == null) {
            return false;
        }

        // 시작 노드가 대상일 경우
        if (targetNodeId.equals(startNodeId)) {
            return true;
        }

        // BFS로 연결 확인
        Queue<String> queue = new LinkedList<>();
        Set<String> visited = new HashSet<>();

        queue.add(startNodeId);
        visited.add(startNodeId);

        while (!queue.isEmpty()) {
            String current = queue.poll();

            for (String adjacent : getAdjacentNodes(current)) {
                if (visited.contains(adjacent)) {
                    continue;
                }

                if (adjacent.equals(targetNodeId)) {
                    return true;
                }

                // 활성화된 노드만 통과 가능
                if (activatedNodes.contains(adjacent)) {
                    queue.add(adjacent);
                    visited.add(adjacent);
                }
            }
        }

        return false;
    }

    // ===== Getters =====

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public int getWidth() {
        return width;
    }

    public int getHeight() {
        return height;
    }

    public Collection<ParagonNode> getAllNodes() {
        return nodes.values();
    }

    public int getNodeCount() {
        return nodes.size();
    }

    public String getStartNodeId() {
        return startNodeId;
    }

    public void setStartNodeId(String startNodeId) {
        this.startNodeId = startNodeId;
    }

    /**
     * 타입별 노드 수를 반환합니다.
     */
    public Map<ParagonNode.NodeType, Integer> getNodeTypeCounts() {
        Map<ParagonNode.NodeType, Integer> counts = new EnumMap<>(ParagonNode.NodeType.class);
        for (ParagonNode node : nodes.values()) {
            counts.merge(node.getType(), 1, Integer::sum);
        }
        return counts;
    }
}
