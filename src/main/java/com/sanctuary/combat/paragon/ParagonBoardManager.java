package com.sanctuary.combat.paragon;

import com.sanctuary.core.ecs.EntityManager;
import com.sanctuary.core.ecs.SanctuaryEntity;
import com.sanctuary.core.ecs.component.AttributeComponent;

import java.util.*;
import java.util.logging.Logger;

/**
 * 정복자 보드 관리자
 * 보드 로드, 노드 활성화, 스탯 부여를 담당합니다.
 */
public class ParagonBoardManager {

    private final Logger logger;
    private final EntityManager entityManager;

    // 보드 저장소
    private final Map<String, ParagonBoard> boards = new HashMap<>();

    public ParagonBoardManager(Logger logger, EntityManager entityManager) {
        this.logger = logger;
        this.entityManager = entityManager;

        // 기본 시작 보드 생성
        createStarterBoard();
    }

    /**
     * 기본 시작 보드를 생성합니다.
     */
    private void createStarterBoard() {
        ParagonBoard board = new ParagonBoard("starter_board", "시작 보드", 7, 7);

        // 중앙 시작 노드
        ParagonNode start = new ParagonNode("start", ParagonNode.NodeType.NORMAL, "NONE", 0, 3, 3);
        board.addNode(start);
        board.setStartNodeId("start");

        // 주변 일반 노드
        board.addNode(new ParagonNode("str_1", ParagonNode.NodeType.NORMAL, "STRENGTH", 5, 2, 3));
        board.addNode(new ParagonNode("str_2", ParagonNode.NodeType.NORMAL, "STRENGTH", 5, 4, 3));
        board.addNode(new ParagonNode("dex_1", ParagonNode.NodeType.NORMAL, "DEXTERITY", 5, 3, 2));
        board.addNode(new ParagonNode("dex_2", ParagonNode.NodeType.NORMAL, "DEXTERITY", 5, 3, 4));
        board.addNode(new ParagonNode("int_1", ParagonNode.NodeType.NORMAL, "INTELLIGENCE", 5, 2, 2));
        board.addNode(new ParagonNode("wil_1", ParagonNode.NodeType.NORMAL, "WILLPOWER", 5, 4, 2));

        // 마법 노드
        board.addNode(new ParagonNode("magic_crit", ParagonNode.NodeType.MAGIC, "CRIT_CHANCE", 0.02, 1, 3));
        board.addNode(new ParagonNode("magic_res", ParagonNode.NodeType.MAGIC, "ALL_RESISTANCE", 0.05, 5, 3));

        // 희귀 노드
        board.addNode(new ParagonNode("rare_dmg", ParagonNode.NodeType.RARE, "DAMAGE_BONUS", 0.10,
                "rare_damage_bonus", 3, 1));

        // 문양 소켓
        board.addNode(new ParagonNode("glyph_1", ParagonNode.NodeType.GLYPH_SOCKET, "GLYPH", 0, 3, 5));

        // 연결 설정
        board.connect("start", "str_1");
        board.connect("start", "str_2");
        board.connect("start", "dex_1");
        board.connect("start", "dex_2");
        board.connect("str_1", "int_1");
        board.connect("str_2", "wil_1");
        board.connect("str_1", "magic_crit");
        board.connect("str_2", "magic_res");
        board.connect("dex_1", "rare_dmg");
        board.connect("dex_2", "glyph_1");

        boards.put(board.getId(), board);
        logger.info("[ParagonBoardManager] 시작 보드 생성 완료: " + board.getNodeCount() + "개 노드");
    }

    /**
     * 보드를 반환합니다.
     */
    public ParagonBoard getBoard(String boardId) {
        return boards.get(boardId);
    }

    /**
     * 노드를 활성화할 수 있는지 확인합니다.
     */
    public boolean canActivateNode(SanctuaryEntity player, String boardId, String nodeId) {
        ParagonBoard board = boards.get(boardId);
        if (board == null) {
            return false;
        }

        ParagonNode node = board.getNode(nodeId);
        if (node == null) {
            return false;
        }

        ParagonComponent paragon = player.getComponent(ParagonComponent.class);
        if (paragon == null) {
            return false;
        }

        // 이미 활성화됨
        if (paragon.isNodeActivated(boardId, nodeId)) {
            return false;
        }

        // 포인트 확인
        if (paragon.getAvailablePoints() < node.getPointCost()) {
            return false;
        }

        // 연결 확인 (시작 노드에서 도달 가능해야 함)
        Set<String> activated = paragon.getActivatedNodes(boardId);
        return board.isReachable(nodeId, activated) || nodeId.equals(board.getStartNodeId());
    }

    /**
     * 노드를 활성화합니다.
     */
    public ActivateResult activateNode(SanctuaryEntity player, String boardId, String nodeId) {
        if (!canActivateNode(player, boardId, nodeId)) {
            return ActivateResult.FAIL_REQUIREMENTS;
        }

        ParagonBoard board = boards.get(boardId);
        ParagonNode node = board.getNode(nodeId);
        ParagonComponent paragon = player.getComponent(ParagonComponent.class);

        // 노드 활성화
        if (!paragon.activateNode(boardId, nodeId, node.getPointCost())) {
            return ActivateResult.FAIL_NO_POINTS;
        }

        // 스탯 부여
        applyNodeStats(player, node);

        logger.fine("[ParagonBoard] 노드 활성화: " + nodeId + " by " + player.getUuid());
        return ActivateResult.SUCCESS;
    }

    /**
     * 노드 스탯을 적용합니다.
     */
    private void applyNodeStats(SanctuaryEntity player, ParagonNode node) {
        if (node.getStat() == null || node.getStat().equals("NONE") || node.getStat().equals("GLYPH")) {
            return;
        }

        AttributeComponent attr = player.getComponent(AttributeComponent.class);
        if (attr == null) {
            return;
        }

        // 스탯 이름을 Stat enum으로 변환하여 적용
        // TODO: StatValue에 추가하는 로직
        // attr.addFlat(Stat.valueOf(node.getStat()), node.getValue());
    }

    /**
     * 문양을 장착합니다.
     */
    public boolean equipGlyph(SanctuaryEntity player, String boardId, String socketNodeId, String glyphId) {
        ParagonBoard board = boards.get(boardId);
        if (board == null) {
            return false;
        }

        ParagonNode socket = board.getNode(socketNodeId);
        if (socket == null || socket.getType() != ParagonNode.NodeType.GLYPH_SOCKET) {
            return false;
        }

        ParagonComponent paragon = player.getComponent(ParagonComponent.class);
        if (paragon == null || !paragon.isNodeActivated(boardId, socketNodeId)) {
            return false;
        }

        paragon.equipGlyph(socketNodeId, glyphId);
        logger.fine("[ParagonBoard] 문양 장착: " + glyphId + " -> " + socketNodeId);
        return true;
    }

    /**
     * 레벨 50 이상 경험치로 정복자 포인트를 부여합니다.
     */
    public void onExperienceGain(SanctuaryEntity player, int playerLevel, double experience) {
        if (playerLevel < 50) {
            return;
        }

        ParagonComponent paragon = player.getComponent(ParagonComponent.class);
        if (paragon == null) {
            paragon = new ParagonComponent();
            player.attach(paragon);
        }

        // 경험치 → 정복자 포인트 변환 (예: 1000 XP = 1 포인트)
        int points = (int) (experience / 1000);
        if (points > 0) {
            paragon.addParagonPoints(points);
        }
    }

    /**
     * 활성화 결과
     */
    public enum ActivateResult {
        SUCCESS,
        FAIL_NO_POINTS,
        FAIL_REQUIREMENTS,
        FAIL_ALREADY_ACTIVE,
        FAIL_NOT_CONNECTED
    }
}
