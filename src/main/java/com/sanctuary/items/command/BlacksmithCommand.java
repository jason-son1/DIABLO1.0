package com.sanctuary.items.command;

import com.sanctuary.DiabloPlugin;
import com.sanctuary.items.SanctuaryItems;
import com.sanctuary.items.gui.BlacksmithUI;
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
 * 대장간 명령어입니다.
 *
 * 사용법:
 * - /blacksmith - 대장간 GUI 열기
 */
public class BlacksmithCommand implements CommandExecutor, TabCompleter {

    private static final Component PREFIX = Component.text("[대장간] ", NamedTextColor.DARK_GRAY);

    private final DiabloPlugin plugin;
    private final SanctuaryItems items;

    public BlacksmithCommand(DiabloPlugin plugin, SanctuaryItems items) {
        this.plugin = plugin;
        this.items = items;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(PREFIX.append(Component.text("플레이어만 사용할 수 있습니다.")));
            return true;
        }

        // 대장간 GUI 열기
        BlacksmithUI blacksmithUI = items.getBlacksmithUI();
        if (blacksmithUI == null) {
            player.sendMessage(PREFIX.append(Component.text("대장간 시스템이 초기화되지 않았습니다.", NamedTextColor.RED)));
            return true;
        }

        blacksmithUI.open(player);
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        return new ArrayList<>();
    }
}
