package com.sanctuary.world.event;

import com.sanctuary.DiabloPlugin;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

/**
 * 지옥물결 테스트 명령어
 * /helltide start [zone] - 지옥물결 시작
 * /helltide end - 지옥물결 종료
 * /helltide status - 상태 확인
 * /helltide zones - 구역 목록
 */
public class HelltideCommand implements CommandExecutor {

    private final HelltideManager helltideManager;

    public HelltideCommand(HelltideManager helltideManager) {
        this.helltideManager = helltideManager;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0) {
            showHelp(sender);
            return true;
        }

        String subCommand = args[0].toLowerCase();

        switch (subCommand) {
            case "start" -> {
                String zone = args.length >= 2 ? args[1] : "fractured_peaks";
                helltideManager.startHelltide(zone);
                sender.sendMessage("§a지옥물결 시작 명령 전송: " + zone);
            }
            case "end" -> {
                helltideManager.endHelltide();
                sender.sendMessage("§a지옥물결 종료됨.");
            }
            case "status" -> showStatus(sender);
            case "zones" -> showZones(sender);
            case "check" -> {
                if (sender instanceof Player player) {
                    boolean inZone = helltideManager.isInHelltideZone(player);
                    player.sendMessage(inZone ? "§c지옥물결 구역 내에 있습니다!" : "§7지옥물결 구역 외부입니다.");
                }
            }
            default -> showHelp(sender);
        }

        return true;
    }

    private void showHelp(CommandSender sender) {
        sender.sendMessage("§6===== 지옥물결 명령어 =====");
        sender.sendMessage("§e/helltide start [zone] §7- 지옥물결 시작");
        sender.sendMessage("§e/helltide end §7- 지옥물결 종료");
        sender.sendMessage("§e/helltide status §7- 상태 확인");
        sender.sendMessage("§e/helltide zones §7- 구역 목록");
        sender.sendMessage("§e/helltide check §7- 현재 위치 확인");
    }

    private void showStatus(CommandSender sender) {
        sender.sendMessage("§6===== 지옥물결 상태 =====");
        if (helltideManager.isActive()) {
            sender.sendMessage("§e상태: §c활성화");
            sender.sendMessage("§e구역: §f" + helltideManager.getCurrentActiveZone());
            sender.sendMessage("§e보상 배율: §f" + helltideManager.getLootMultiplier() + "x");
            sender.sendMessage("§e레벨 보너스: §f+" + helltideManager.getMonsterLevelBonus());
        } else {
            sender.sendMessage("§e상태: §7비활성");
        }
    }

    private void showZones(CommandSender sender) {
        sender.sendMessage("§6===== 지옥물결 구역 =====");
        sender.sendMessage("§e- fractured_peaks §7부서진 봉우리");
        sender.sendMessage("§e- scosglen §7스코스글렌");
        sender.sendMessage("§e- dry_steppes §7건조한 대초원");
        sender.sendMessage("§e- kehjistan §7케지스탄");
    }
}
