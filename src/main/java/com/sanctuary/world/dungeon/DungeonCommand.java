package com.sanctuary.world.dungeon;

import com.sanctuary.DiabloPlugin;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

/**
 * 던전 시스템 테스트 명령어
 * /dungeon create <templateId> [tier] - 던전 생성
 * /dungeon enter <instanceId> - 던전 입장
 * /dungeon leave - 던전 퇴장
 * /dungeon info - 현재 던전 정보
 * /dungeon list - 템플릿 목록
 */
public class DungeonCommand implements CommandExecutor {

    private final DiabloPlugin plugin;
    private final DungeonManager dungeonManager;

    public DungeonCommand(DiabloPlugin plugin, DungeonManager dungeonManager) {
        this.plugin = plugin;
        this.dungeonManager = dungeonManager;
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
            case "create" -> {
                if (args.length < 2) {
                    player.sendMessage("§c사용법: /dungeon create <templateId> [tier]");
                    return true;
                }
                int tier = args.length >= 3 ? parseInt(args[2], 1) : 1;
                createDungeon(player, args[1], tier);
            }
            case "enter" -> {
                if (args.length < 2) {
                    player.sendMessage("§c사용법: /dungeon enter <instanceId>");
                    return true;
                }
                enterDungeon(player, args[1]);
            }
            case "leave" -> leaveDungeon(player);
            case "info" -> showInfo(player);
            case "list" -> listTemplates(player);
            default -> showHelp(player);
        }

        return true;
    }

    private void showHelp(Player player) {
        player.sendMessage("§6===== 던전 명령어 =====");
        player.sendMessage("§e/dungeon create <id> [tier] §7- 던전 생성");
        player.sendMessage("§e/dungeon enter <instanceId> §7- 던전 입장");
        player.sendMessage("§e/dungeon leave §7- 던전 퇴장");
        player.sendMessage("§e/dungeon info §7- 현재 던전 정보");
        player.sendMessage("§e/dungeon list §7- 템플릿 목록");
    }

    private void createDungeon(Player player, String templateId, int tier) {
        DungeonInstance instance = dungeonManager.createInstance(templateId, tier, player);
        if (instance != null) {
            player.sendMessage("§a던전 생성 중: " + templateId + " (티어 " + tier + ")");
            player.sendMessage("§7인스턴스 ID: " + instance.getInstanceId());
            player.sendMessage("§7잠시 후 '/dungeon enter " + instance.getInstanceId() + "' 로 입장하세요.");
        } else {
            player.sendMessage("§c던전 생성 실패! 템플릿을 확인하세요.");
        }
    }

    private void enterDungeon(Player player, String instanceId) {
        DungeonInstance instance = dungeonManager.getInstance(instanceId);
        if (instance == null) {
            player.sendMessage("§c존재하지 않는 던전입니다.");
            return;
        }

        if (instance.getState() != DungeonInstance.State.READY &&
                instance.getState() != DungeonInstance.State.IN_PROGRESS) {
            player.sendMessage("§c던전이 아직 준비되지 않았거나 이미 종료되었습니다. (상태: " + instance.getState() + ")");
            return;
        }

        if (dungeonManager.enterDungeon(player, instanceId)) {
            player.sendMessage("§a던전에 입장했습니다!");
            player.sendMessage("§7" + instance.getTemplate().getName() + " (티어 " + instance.getTier() + ")");
        } else {
            player.sendMessage("§c던전 입장에 실패했습니다.");
        }
    }

    private void leaveDungeon(Player player) {
        DungeonInstance current = dungeonManager.getPlayerDungeon(player.getUniqueId());
        if (current == null) {
            player.sendMessage("§c현재 던전에 있지 않습니다.");
            return;
        }

        dungeonManager.leaveDungeon(player);
        player.sendMessage("§a던전에서 퇴장했습니다.");
    }

    private void showInfo(Player player) {
        DungeonInstance instance = dungeonManager.getPlayerDungeon(player.getUniqueId());
        if (instance == null) {
            player.sendMessage("§c현재 던전에 있지 않습니다.");
            return;
        }

        DungeonTemplate template = instance.getTemplate();
        player.sendMessage("§6===== 던전 정보 =====");
        player.sendMessage("§e이름: §f" + template.getName());
        player.sendMessage("§e티어: §f" + instance.getTier());
        player.sendMessage("§e상태: §f" + instance.getState());
        player.sendMessage("§e진행도: §f" + String.format("%.1f%%", instance.getProgress() * 100));
        player.sendMessage("§e처치: §f" + instance.getKilledEnemies() + "/" + instance.getTotalEnemies());
        player.sendMessage("§e플레이어: §f" + instance.getPlayerCount() + "명");
        player.sendMessage("§e시간: §f" + formatTime(instance.getElapsedSeconds()));
    }

    private void listTemplates(Player player) {
        player.sendMessage("§6===== 던전 템플릿 =====");
        player.sendMessage("§7(현재 하드코딩된 테스트 템플릿만 제공)");
        player.sendMessage("§e- nightmare_tomb §7잊혀진 무덤 (티어 1-100)");
        player.sendMessage("§e- fallen_temple §7타락한 신전 (티어 1-50)");
    }

    private String formatTime(long seconds) {
        long min = seconds / 60;
        long sec = seconds % 60;
        return String.format("%02d:%02d", min, sec);
    }

    private int parseInt(String str, int defaultValue) {
        try {
            return Integer.parseInt(str);
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }
}
