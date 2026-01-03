package com.sanctuary.items.command;

import com.sanctuary.DiabloPlugin;
import com.sanctuary.items.SanctuaryItems;
import com.sanctuary.items.model.*;
import com.sanctuary.items.serializer.ItemSerializer;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * 아이템 시스템 테스트용 명령어입니다.
 * 
 * 사용법:
 * - /itemtest give <템플릿> [희귀도] [위력] - 아이템 지급
 * - /itemtest inspect - 들고 있는 아이템의 데이터 확인
 * - /itemtest tempering - 담금질 시뮬레이션
 * - /itemtest masterwork - 명품화 시뮬레이션
 */
public class ItemTestCommand implements CommandExecutor, TabCompleter {

    private static final String PREFIX = ChatColor.LIGHT_PURPLE + "[Items] " + ChatColor.RESET;

    private final DiabloPlugin plugin;
    private final SanctuaryItems items;

    public ItemTestCommand(DiabloPlugin plugin, SanctuaryItems items) {
        this.plugin = plugin;
        this.items = items;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(PREFIX + "플레이어만 사용할 수 있습니다.");
            return true;
        }

        if (!player.hasPermission("sanctuary.admin")) {
            player.sendMessage(PREFIX + ChatColor.RED + "권한이 없습니다.");
            return true;
        }

        if (args.length == 0) {
            sendHelp(player);
            return true;
        }

        String subCommand = args[0].toLowerCase();

        switch (subCommand) {
            case "give" -> handleGive(player, args);
            case "inspect" -> handleInspect(player);
            case "tempering" -> handleTempering(player);
            case "masterwork" -> handleMasterwork(player);
            case "demo" -> handleDemo(player);
            default -> sendHelp(player);
        }

        return true;
    }

    private void handleGive(Player player, String[] args) {
        String template = args.length > 1 ? args[1] : "iron_sword";
        ItemRarity rarity = ItemRarity.RARE;
        int itemPower = 100;

        if (args.length > 2) {
            try {
                rarity = ItemRarity.valueOf(args[2].toUpperCase());
            } catch (IllegalArgumentException e) {
                player.sendMessage(PREFIX + ChatColor.RED + "올바르지 않은 희귀도입니다.");
                return;
            }
        }

        if (args.length > 3) {
            try {
                itemPower = Integer.parseInt(args[3]);
            } catch (NumberFormatException e) {
                player.sendMessage(PREFIX + ChatColor.RED + "올바르지 않은 위력 값입니다.");
                return;
            }
        }

        ItemStack item = items.getItemFactory().create(template, rarity, itemPower);
        if (item != null) {
            player.getInventory().addItem(item);
            player.sendMessage(PREFIX + ChatColor.GREEN + "아이템이 지급되었습니다!");
        } else {
            player.sendMessage(PREFIX + ChatColor.RED + "아이템 생성에 실패했습니다.");
        }
    }

    private void handleInspect(Player player) {
        ItemStack item = player.getInventory().getItemInMainHand();
        if (item.getType().isAir()) {
            player.sendMessage(PREFIX + ChatColor.GRAY + "손에 아이템을 들어주세요.");
            return;
        }

        RpgItemData data = items.getSerializer().read(item);
        if (data == null) {
            player.sendMessage(PREFIX + ChatColor.GRAY + "Sanctuary 아이템이 아닙니다.");
            return;
        }

        player.sendMessage(PREFIX + ChatColor.AQUA + "=== 아이템 데이터 ===");
        player.sendMessage(ChatColor.GRAY + "UUID: " + ChatColor.WHITE + data.getUuid());
        player.sendMessage(ChatColor.GRAY + "템플릿: " + ChatColor.WHITE + data.getTemplateId());
        player.sendMessage(ChatColor.GRAY + "희귀도: " + data.getRarity().getColoredName());
        player.sendMessage(ChatColor.GRAY + "위력: " + ChatColor.WHITE + data.getItemPower());
        player.sendMessage(ChatColor.GRAY + "어픽스: " + ChatColor.WHITE + data.getExplicitAffixes().size() + "개");

        if (data.getGreaterAffixCount() > 0) {
            player.sendMessage(ChatColor.RED + "  GA: " + data.getGreaterAffixCount() + "개");
        }

        for (AffixInstance affix : data.getExplicitAffixes()) {
            String gaTag = affix.isGreater() ? ChatColor.RED + " [GA]" : "";
            player.sendMessage("  " + ChatColor.WHITE + affix.getStatKey() + ": " +
                    String.format("%.2f", affix.getValue()) + gaTag);
        }
    }

    private void handleTempering(Player player) {
        ItemStack item = player.getInventory().getItemInMainHand();
        RpgItemData data = items.getSerializer().read(item);

        if (data == null) {
            player.sendMessage(PREFIX + ChatColor.GRAY + "Sanctuary 아이템이 아닙니다.");
            return;
        }

        if (!data.isLegendaryOrHigher()) {
            player.sendMessage(PREFIX + ChatColor.RED + "전설 등급 이상만 담금질이 가능합니다.");
            return;
        }

        TemperingData tempering = data.getTempering();
        if (!tempering.canTemper()) {
            player.sendMessage(PREFIX + ChatColor.RED + "담금질 내구도가 소진되었습니다.");
            return;
        }

        // 랜덤 담금질 어픽스 추가
        AffixInstance newAffix = new AffixInstance("TEMPERING_CRIT", "CRIT_DAMAGE",
                Math.random() * 0.1 + 0.05);

        if (tempering.getSlot1() == null) {
            tempering.setSlot1(newAffix);
        } else {
            tempering.setSlot2(newAffix);
        }
        tempering.consumeDurability();

        items.getSerializer().write(item, data);
        items.getLoreGenerator().applyLore(item, data);

        player.sendMessage(PREFIX + ChatColor.GREEN + "담금질 완료! (남은 내구도: " +
                tempering.getDurability() + ")");
    }

    private void handleMasterwork(Player player) {
        ItemStack item = player.getInventory().getItemInMainHand();
        RpgItemData data = items.getSerializer().read(item);

        if (data == null) {
            player.sendMessage(PREFIX + ChatColor.GRAY + "Sanctuary 아이템이 아닙니다.");
            return;
        }

        MasterworkingData mw = data.getMasterworking();
        if (!mw.canUpgrade()) {
            player.sendMessage(PREFIX + ChatColor.RED + "이미 최대 명품화 랭크입니다.");
            return;
        }

        boolean isCritical = mw.upgrade();
        if (isCritical) {
            // 랜덤 어픽스에 크리티컬 적용
            int affixCount = data.getExplicitAffixes().size();
            if (affixCount > 0) {
                int critIndex = (int) (Math.random() * affixCount);
                mw.addCritical(critIndex);
                player.sendMessage(PREFIX + ChatColor.YELLOW + "★ 크리티컬! 어픽스 #" +
                        (critIndex + 1) + " 강화!");
            }
        }

        items.getSerializer().write(item, data);
        items.getLoreGenerator().applyLore(item, data);

        player.sendMessage(PREFIX + ChatColor.GREEN + "명품화 완료! (랭크: " +
                mw.getRank() + "/12)");
    }

    private void handleDemo(Player player) {
        // 데모용 전설 아이템 생성
        RpgItemData data = new RpgItemData("demo_sword", ItemRarity.LEGENDARY, 150);
        data.setDisplayName("리릿스의 검");
        data.setRequiredLevel(60);

        // 암시적 어픽스
        data.getImplicitAffixes().add(new AffixInstance("IMPLICIT_WD", "WEAPON_DAMAGE", 250));

        // 명시적 어픽스 (GA 포함)
        data.getExplicitAffixes().add(AffixInstance.createGreater("GA_CRIT", "CRIT_DAMAGE", 0.15));
        data.getExplicitAffixes().add(new AffixInstance("AFFIX_STR", "STRENGTH", 150));
        data.getExplicitAffixes().add(new AffixInstance("AFFIX_CS", "CRIT_CHANCE", 0.08));
        data.getExplicitAffixes().add(new AffixInstance("AFFIX_AS", "ATTACK_SPEED", 0.12));

        // 담금질
        data.getTempering().setSlot1(new AffixInstance("TEMP_VD", "VULNERABLE_DAMAGE", 0.15));

        // 명품화
        data.getMasterworking().setRank(8);
        data.getMasterworking().addCritical(0);
        data.getMasterworking().addCritical(2);

        // 위상
        data.setAspectId("쟁취자의 위상");
        data.setAspectValue(0.25);

        ItemStack item = new ItemStack(Material.NETHERITE_SWORD);
        items.getSerializer().write(item, data);
        items.getLoreGenerator().applyLore(item, data);

        player.getInventory().addItem(item);
        player.sendMessage(PREFIX + ChatColor.GOLD + "데모 전설 아이템이 지급되었습니다!");
    }

    private void sendHelp(Player player) {
        player.sendMessage(PREFIX + ChatColor.AQUA + "=== Item Test 명령어 ===");
        player.sendMessage(ChatColor.GRAY + "/itemtest give <템플릿> [희귀도] [위력]" +
                ChatColor.WHITE + " - 아이템 지급");
        player.sendMessage(ChatColor.GRAY + "/itemtest inspect" +
                ChatColor.WHITE + " - 아이템 데이터 확인");
        player.sendMessage(ChatColor.GRAY + "/itemtest tempering" +
                ChatColor.WHITE + " - 담금질 시뮬레이션");
        player.sendMessage(ChatColor.GRAY + "/itemtest masterwork" +
                ChatColor.WHITE + " - 명품화 시뮬레이션");
        player.sendMessage(ChatColor.GRAY + "/itemtest demo" +
                ChatColor.WHITE + " - 데모 전설 아이템");
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            return filterCompletions(args[0], "give", "inspect", "tempering", "masterwork", "demo");
        }
        if (args.length == 2 && args[0].equalsIgnoreCase("give")) {
            return filterCompletions(args[1], "iron_sword", "diamond_sword", "leather_helmet");
        }
        if (args.length == 3 && args[0].equalsIgnoreCase("give")) {
            return filterCompletions(args[2],
                    Arrays.stream(ItemRarity.values()).map(Enum::name).toArray(String[]::new));
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
