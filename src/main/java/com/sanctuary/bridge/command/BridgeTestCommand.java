package com.sanctuary.bridge.command;

import com.sanctuary.DiabloPlugin;
import com.sanctuary.bridge.SanctuaryBridge;
import com.sanctuary.bridge.packet.SanctuaryPacket;
import com.sanctuary.bridge.sdui.SduiComponent;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;

/**
 * 브릿지 시스템 테스트용 명령어입니다.
 * 
 * 사용법:
 * - /bridgetest status - 모드 클라이언트 상태 확인
 * - /bridgetest sync - 스탯 수동 동기화
 * - /bridgetest bossbar <이름> <HP> - 보스바 테스트
 * - /bridgetest damage <량> - 데미지 인디케이터 테스트
 */
public class BridgeTestCommand implements CommandExecutor, TabCompleter {

    private static final Component PREFIX = Component.text("[Bridge] ", NamedTextColor.DARK_PURPLE);

    private final DiabloPlugin plugin;
    private final SanctuaryBridge bridge;

    public BridgeTestCommand(DiabloPlugin plugin, SanctuaryBridge bridge) {
        this.plugin = plugin;
        this.bridge = bridge;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(PREFIX.append(Component.text("플레이어만 사용할 수 있습니다.")));
            return true;
        }

        if (!player.hasPermission("sanctuary.admin")) {
            player.sendMessage(PREFIX.append(Component.text("권한이 없습니다.", NamedTextColor.RED)));
            return true;
        }

        if (args.length == 0) {
            sendHelp(player);
            return true;
        }

        String subCommand = args[0].toLowerCase();

        switch (subCommand) {
            case "status" -> handleStatus(player);
            case "sync" -> handleSync(player);
            case "bossbar" -> handleBossBar(player, args);
            case "damage" -> handleDamage(player, args);
            case "hud" -> handleHud(player);
            default -> sendHelp(player);
        }

        return true;
    }

    private void handleStatus(Player player) {
        player.sendMessage(PREFIX.append(Component.text("=== Bridge 상태 ===", NamedTextColor.AQUA)));

        int modClients = bridge.getPacketManager().getModClientCount();
        player.sendMessage(Component.text("모드 클라이언트: ", NamedTextColor.GRAY)
                .append(Component.text(modClients + "명", NamedTextColor.WHITE)));

        boolean hasModClient = bridge.getPacketManager().hasModClient(player);
        player.sendMessage(Component.text("본인 모드: ", NamedTextColor.GRAY)
                .append(hasModClient ? Component.text("연결됨", NamedTextColor.GREEN)
                        : Component.text("바닐라", NamedTextColor.RED)));

        player.sendMessage(Component.text("채널: ", NamedTextColor.GRAY)
                .append(Component.text(com.sanctuary.bridge.packet.PacketManager.CHANNEL, NamedTextColor.WHITE)));
    }

    private void handleSync(Player player) {
        if (!bridge.getPacketManager().hasModClient(player)) {
            player.sendMessage(PREFIX.append(Component.text("모드 클라이언트가 연결되지 않았습니다.", NamedTextColor.GRAY)));
            player.sendMessage(Component.text("(동기화가 전송되었지만 수신할 클라이언트가 없습니다)", NamedTextColor.GRAY));
        }

        bridge.getStatSyncService().syncStats(player);
        bridge.getStatSyncService().syncStatusEffects(player);

        player.sendMessage(PREFIX.append(Component.text("스탯 동기화가 전송되었습니다.", NamedTextColor.GREEN)));
    }

    private void handleBossBar(Player player, String[] args) {
        String bossName = args.length > 1 ? args[1] : "테스트 보스";
        double hp = args.length > 2 ? Double.parseDouble(args[2]) : 5000.0;
        double maxHp = args.length > 3 ? Double.parseDouble(args[3]) : 10000.0;

        bridge.getStatSyncService().sendBossBar(player, bossName, hp, maxHp, 30, 100);

        player.sendMessage(PREFIX.append(Component.text("보스바 패킷이 전송되었습니다.", NamedTextColor.GREEN)));
        player.sendMessage(Component.text("보스: " + bossName + " | HP: " + hp + "/" + maxHp, NamedTextColor.GRAY));
    }

    private void handleDamage(Player player, String[] args) {
        double damage = args.length > 1 ? Double.parseDouble(args[1]) : 1234.0;
        boolean crit = args.length > 2 && args[2].equalsIgnoreCase("true");
        boolean overpower = args.length > 3 && args[3].equalsIgnoreCase("true");

        var loc = player.getLocation();
        bridge.getStatSyncService().sendDamageIndicator(player, damage, crit, overpower,
                loc.getX(), loc.getY() + 2, loc.getZ());

        player.sendMessage(PREFIX.append(Component.text("데미지 인디케이터가 전송되었습니다.", NamedTextColor.GREEN)));
        player.sendMessage(Component.text("데미지: " + damage +
                (crit ? " (치명타)" : "") + (overpower ? " (제압)" : ""), NamedTextColor.GRAY));
    }

    private void handleHud(Player player) {
        bridge.getStatSyncService().sendHudUpdate(player);
        player.sendMessage(PREFIX.append(Component.text("HUD 업데이트가 전송되었습니다.", NamedTextColor.GREEN)));
    }

    private void sendHelp(Player player) {
        player.sendMessage(PREFIX.append(Component.text("=== Bridge Test 명령어 ===", NamedTextColor.AQUA)));
        player.sendMessage(Component.text("/bridgetest status", NamedTextColor.GRAY)
                .append(Component.text(" - 연결 상태 확인", NamedTextColor.WHITE)));
        player.sendMessage(Component.text("/bridgetest sync", NamedTextColor.GRAY)
                .append(Component.text(" - 스탯 동기화", NamedTextColor.WHITE)));
        player.sendMessage(Component.text("/bridgetest bossbar <이름> [HP] [MaxHP]", NamedTextColor.GRAY)
                .append(Component.text(" - 보스바 테스트", NamedTextColor.WHITE)));
        player.sendMessage(Component.text("/bridgetest damage <량> [crit] [overpower]", NamedTextColor.GRAY)
                .append(Component.text(" - 데미지 테스트", NamedTextColor.WHITE)));
        player.sendMessage(Component.text("/bridgetest hud", NamedTextColor.GRAY)
                .append(Component.text(" - HUD 전송", NamedTextColor.WHITE)));
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            return filterCompletions(args[0], "status", "sync", "bossbar", "damage", "hud");
        }
        return new ArrayList<>();
    }

    private List<String> filterCompletions(String input, String... options) {
        List<String> result = new ArrayList<>();
        String lowerInput = input.toLowerCase();
        for (String option : options) {
            if (option.toLowerCase().startsWith(lowerInput)) {
                result.add(option);
            }
        }
        return result;
    }
}
