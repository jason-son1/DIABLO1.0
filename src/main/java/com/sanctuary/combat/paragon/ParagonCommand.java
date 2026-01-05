package com.sanctuary.combat.paragon;

import com.sanctuary.combat.SanctuaryCombat;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

/**
 * 정복자 보드 테스트 명령어
 * /paragon info - 현재 상태
 * /paragon activate <nodeId> - 노드 활성화
 * /paragon board <boardId> - 보드 전환
 * /paragon addpoints <n> - 포인트 추가 (테스트)
 * /paragon reset - 전체 초기화
 */
public class ParagonCommand implements CommandExecutor {

    private final SanctuaryCombat combatModule;
    private final ParagonBoardManager boardManager;

    public ParagonCommand(SanctuaryCombat combatModule, ParagonBoardManager boardManager) {
        this.combatModule = combatModule;
        this.boardManager = boardManager;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("§c플레이어만 사용할 수 있습니다.");
            return true;
        }

        if (args.length == 0) {
            showHelp(player);
            return true;
        }

        String subCommand = args[0].toLowerCase();

        switch (subCommand) {
            case "info" -> showInfo(player);
            case "activate" -> {
                if (args.length < 2) {
                    player.sendMessage("§c사용법: /paragon activate <nodeId>");
                    return true;
                }
                activateNode(player, args[1]);
            }
            case "board" -> {
                if (args.length < 2) {
                    player.sendMessage("§c사용법: /paragon board <boardId>");
                    return true;
                }
                switchBoard(player, args[1]);
            }
            case "addpoints" -> {
                if (args.length < 2) {
                    player.sendMessage("§c사용법: /paragon addpoints <amount>");
                    return true;
                }
                addPoints(player, args[1]);
            }
            case "reset" -> resetParagon(player);
            case "nodes" -> listNodes(player);
            default -> showHelp(player);
        }

        return true;
    }

    private void showHelp(Player player) {
        player.sendMessage("§6===== 정복자 보드 명령어 =====");
        player.sendMessage("§e/paragon info §7- 현재 상태");
        player.sendMessage("§e/paragon activate <id> §7- 노드 활성화");
        player.sendMessage("§e/paragon board <id> §7- 보드 전환");
        player.sendMessage("§e/paragon nodes §7- 현재 보드 노드 목록");
        player.sendMessage("§e/paragon addpoints <n> §7- 포인트 추가 (테스트)");
        player.sendMessage("§e/paragon reset §7- 전체 초기화");
    }

    private void showInfo(Player player) {
        var entity = combatModule.getCore().getEntityManager().get(player.getUniqueId());
        if (entity == null) {
            player.sendMessage("§c엔티티 정보를 찾을 수 없습니다.");
            return;
        }

        ParagonComponent paragon = entity.getComponent(ParagonComponent.class);
        if (paragon == null) {
            player.sendMessage("§c정복자 컴포넌트가 없습니다.");
            return;
        }

        player.sendMessage("§6===== 정복자 보드 상태 =====");
        player.sendMessage("§e총 포인트: §f" + paragon.getTotalParagonPoints());
        player.sendMessage("§e사용된 포인트: §f" + paragon.getUsedPoints());
        player.sendMessage("§e사용 가능: §f" + paragon.getAvailablePoints());
        player.sendMessage("§e활성화된 노드: §f" + paragon.getTotalActivatedNodes());
        player.sendMessage("§e현재 보드: §f" + paragon.getActiveBoardId());
    }

    private void activateNode(Player player, String nodeId) {
        var entity = combatModule.getCore().getEntityManager().get(player.getUniqueId());
        if (entity == null) {
            player.sendMessage("§c엔티티 정보를 찾을 수 없습니다.");
            return;
        }

        ParagonComponent paragon = entity.getComponent(ParagonComponent.class);
        if (paragon == null) {
            player.sendMessage("§c정복자 컴포넌트가 없습니다.");
            return;
        }

        String boardId = paragon.getActiveBoardId();
        var result = boardManager.activateNode(entity, boardId, nodeId);

        switch (result) {
            case SUCCESS -> {
                ParagonBoard board = boardManager.getBoard(boardId);
                ParagonNode node = board.getNode(nodeId);
                player.sendMessage("§a노드 활성화 성공: " + node.getDisplayName());
            }
            case FAIL_NO_POINTS -> player.sendMessage("§c포인트가 부족합니다.");
            case FAIL_REQUIREMENTS -> player.sendMessage("§c활성화할 수 없습니다. 연결된 노드를 먼저 활성화하세요.");
            default -> player.sendMessage("§c활성화 실패: " + result);
        }
    }

    private void switchBoard(Player player, String boardId) {
        var entity = combatModule.getCore().getEntityManager().get(player.getUniqueId());
        if (entity == null) {
            player.sendMessage("§c엔티티 정보를 찾을 수 없습니다.");
            return;
        }

        ParagonBoard board = boardManager.getBoard(boardId);
        if (board == null) {
            player.sendMessage("§c존재하지 않는 보드입니다: " + boardId);
            return;
        }

        ParagonComponent paragon = entity.getComponent(ParagonComponent.class);
        if (paragon == null) {
            paragon = new ParagonComponent();
            entity.attach(paragon);
        }

        paragon.setActiveBoard(boardId);
        player.sendMessage("§a보드 전환: " + board.getName());
    }

    private void listNodes(Player player) {
        var entity = combatModule.getCore().getEntityManager().get(player.getUniqueId());
        if (entity == null) {
            player.sendMessage("§c엔티티 정보를 찾을 수 없습니다.");
            return;
        }

        ParagonComponent paragon = entity.getComponent(ParagonComponent.class);
        String boardId = paragon != null ? paragon.getActiveBoardId() : "starter_board";

        ParagonBoard board = boardManager.getBoard(boardId);
        if (board == null) {
            player.sendMessage("§c보드를 찾을 수 없습니다.");
            return;
        }

        player.sendMessage("§6===== " + board.getName() + " 노드 목록 =====");
        for (ParagonNode node : board.getAllNodes()) {
            boolean activated = paragon != null && paragon.isNodeActivated(boardId, node.getId());
            String status = activated ? "§a[활성]" : "§7[비활성]";
            player.sendMessage(status + " " + node.getDisplayName() + " §7(" + node.getId() + ")");
        }
    }

    private void addPoints(Player player, String amountStr) {
        try {
            int amount = Integer.parseInt(amountStr);
            var entity = combatModule.getCore().getEntityManager().get(player.getUniqueId());
            if (entity == null) {
                player.sendMessage("§c엔티티 정보를 찾을 수 없습니다.");
                return;
            }

            ParagonComponent paragon = entity.getComponent(ParagonComponent.class);
            if (paragon == null) {
                paragon = new ParagonComponent();
                entity.attach(paragon);
            }

            paragon.addParagonPoints(amount);
            player.sendMessage("§a정복자 포인트 " + amount + "점 추가! (현재: " + paragon.getAvailablePoints() + ")");
        } catch (NumberFormatException e) {
            player.sendMessage("§c올바른 숫자를 입력하세요.");
        }
    }

    private void resetParagon(Player player) {
        var entity = combatModule.getCore().getEntityManager().get(player.getUniqueId());
        if (entity == null) {
            player.sendMessage("§c엔티티 정보를 찾을 수 없습니다.");
            return;
        }

        ParagonComponent paragon = entity.getComponent(ParagonComponent.class);
        if (paragon != null) {
            paragon.respecAll();
            player.sendMessage("§a정복자 보드가 초기화되었습니다!");
        }
    }
}
