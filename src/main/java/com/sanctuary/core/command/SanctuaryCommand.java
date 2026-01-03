package com.sanctuary.core.command;

import com.sanctuary.DiabloPlugin;
import com.sanctuary.core.SanctuaryCore;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Sanctuary 플러그인의 메인 명령어 핸들러입니다.
 * 
 * 사용법:
 * - /sanctuary reload - 데이터 및 스크립트 리로드
 * - /sanctuary status - 시스템 상태 확인
 */
public class SanctuaryCommand implements CommandExecutor, TabCompleter {

    private static final Component PREFIX = Component.text("[Sanctuary] ", NamedTextColor.GOLD);

    private final DiabloPlugin plugin;
    private final SanctuaryCore core;

    public SanctuaryCommand(DiabloPlugin plugin, SanctuaryCore core) {
        this.plugin = plugin;
        this.core = core;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("sanctuary.admin")) {
            sender.sendMessage(PREFIX.append(Component.text("권한이 없습니다.", NamedTextColor.RED)));
            return true;
        }

        if (args.length == 0) {
            sendHelp(sender);
            return true;
        }

        String subCommand = args[0].toLowerCase();

        switch (subCommand) {
            case "reload":
                handleReload(sender);
                break;
            case "status":
                handleStatus(sender);
                break;
            default:
                sendHelp(sender);
                break;
        }

        return true;
    }

    private void handleReload(CommandSender sender) {
        sender.sendMessage(PREFIX.append(Component.text("시스템 리로드 중...", NamedTextColor.YELLOW)));

        try {
            long start = System.currentTimeMillis();

            // 데이터 리로드
            core.reload();

            long elapsed = System.currentTimeMillis() - start;
            sender.sendMessage(PREFIX.append(Component.text("리로드 완료! (" + elapsed + "ms)", NamedTextColor.GREEN)));

        } catch (Exception e) {
            sender.sendMessage(PREFIX.append(Component.text("리로드 실패: " + e.getMessage(), NamedTextColor.RED)));
            plugin.getLogger().severe("시스템 리로드 중 오류: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void handleStatus(CommandSender sender) {
        sender.sendMessage(PREFIX.append(Component.text("=== Sanctuary 시스템 상태 ===", NamedTextColor.AQUA)));

        // Core 상태
        sender.sendMessage(
                Component.text("▶ Core: ", NamedTextColor.GRAY).append(Component.text("활성화", NamedTextColor.GREEN)));

        // 데이터 상태
        var dataRepo = core.getDataRepository();
        sender.sendMessage(Component.text("  ├ 스탯 정의: ", NamedTextColor.GRAY)
                .append(Component.text(dataRepo.getAllStats().size() + "개", NamedTextColor.WHITE)));

        // ECS 상태
        var entityManager = core.getEntityManager();
        sender.sendMessage(Component.text("  └ 관리 중인 엔티티: ", NamedTextColor.GRAY)
                .append(Component.text(entityManager.size() + "개", NamedTextColor.WHITE)));

        // 스크립트 상태
        sender.sendMessage(Component.text("▶ Script Engine: ", NamedTextColor.GRAY)
                .append(Component.text("활성화", NamedTextColor.GREEN)));

        // 메모리 상태
        Runtime runtime = Runtime.getRuntime();
        long usedMemory = (runtime.totalMemory() - runtime.freeMemory()) / 1024 / 1024;
        long maxMemory = runtime.maxMemory() / 1024 / 1024;
        sender.sendMessage(Component.text("▶ 메모리: ", NamedTextColor.GRAY)
                .append(Component.text(usedMemory + "MB / " + maxMemory + "MB", NamedTextColor.WHITE)));
    }

    private void sendHelp(CommandSender sender) {
        sender.sendMessage(PREFIX.append(Component.text("=== Sanctuary 명령어 ===", NamedTextColor.AQUA)));
        sender.sendMessage(Component.text("/sanctuary reload", NamedTextColor.GRAY)
                .append(Component.text(" - 데이터 및 스크립트 리로드", NamedTextColor.WHITE)));
        sender.sendMessage(Component.text("/sanctuary status", NamedTextColor.GRAY)
                .append(Component.text(" - 시스템 상태 확인", NamedTextColor.WHITE)));
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            List<String> completions = new ArrayList<>();
            String input = args[0].toLowerCase();

            for (String sub : Arrays.asList("reload", "status")) {
                if (sub.startsWith(input)) {
                    completions.add(sub);
                }
            }
            return completions;
        }
        return new ArrayList<>();
    }
}
