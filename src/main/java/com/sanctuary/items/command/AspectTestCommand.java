package com.sanctuary.items.command;

import com.sanctuary.DiabloPlugin;
import com.sanctuary.core.model.AspectData;
import com.sanctuary.items.SanctuaryItems;
import com.sanctuary.items.aspect.AspectInstance;
import com.sanctuary.items.aspect.AspectManager;
import com.sanctuary.items.aspect.AspectStorage;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 위상(Aspect) 시스템 테스트 명령어입니다.
 *
 * 사용법:
 * - /aspecttest list - 보유한 위상 목록
 * - /aspecttest add <aspectId> - 위상 추가 (테스트용)
 * - /aspecttest clear - 모든 위상 제거
 */
public class AspectTestCommand implements CommandExecutor, TabCompleter {

    private static final Component PREFIX = Component.text("[Aspect] ", NamedTextColor.GOLD);

    private final DiabloPlugin plugin;
    private final SanctuaryItems items;

    public AspectTestCommand(DiabloPlugin plugin, SanctuaryItems items) {
        this.plugin = plugin;
        this.items = items;
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
            case "list" -> handleList(player);
            case "add" -> handleAdd(player, args);
            case "clear" -> handleClear(player);
            case "info" -> handleInfo(player, args);
            default -> sendHelp(player);
        }

        return true;
    }

    private void handleList(Player player) {
        AspectManager aspectManager = items.getAspectManager();
        AspectStorage storage = aspectManager.getStorage(player.getUniqueId());

        player.sendMessage(PREFIX.append(Component.text("=== 보유 위상 목록 ===", NamedTextColor.AQUA)));

        if (storage.getAspectCount() == 0) {
            player.sendMessage(Component.text("  (없음)", NamedTextColor.GRAY));
            return;
        }

        for (AspectInstance aspect : storage.getAllAspects()) {
            String categoryColor = switch (aspect.getCategory()) {
                case "OFFENSIVE" -> "§c";
                case "DEFENSIVE" -> "§9";
                case "UTILITY" -> "§e";
                default -> "§7";
            };

            player.sendMessage(Component.text("  " + categoryColor + "[" + aspect.getCategory() + "]§f " +
                    aspect.getAspectName()));
            player.sendMessage(Component.text("    §7" + aspect.getFormattedDescription()));
        }

        player.sendMessage(Component.text("  총 " + storage.getAspectCount() + "개", NamedTextColor.GRAY));
    }

    private void handleAdd(Player player, String[] args) {
        if (args.length < 2) {
            player.sendMessage(PREFIX.append(Component.text("사용법: /aspecttest add <aspectId>")));
            return;
        }

        String aspectId = args[1];
        AspectManager aspectManager = items.getAspectManager();

        // 데이터에서 위상 조회
        AspectData aspectData = plugin.getCoreModule().getDataRepository().getAspect(aspectId);
        if (aspectData == null) {
            player.sendMessage(PREFIX.append(Component.text("존재하지 않는 위상: " + aspectId, NamedTextColor.RED)));
            return;
        }

        // 수동으로 인스턴스 생성
        AspectInstance instance = new AspectInstance(
                aspectData.getId(),
                aspectData.getName(),
                aspectData.getCategory(),
                aspectData.getDescription(),
                aspectData.getAllowedSlots(),
                aspectData.getClassRestriction());

        // 기본값 롤링 (테스트용)
        if (aspectData.getEffect() != null) {
            for (Map.Entry<String, Object> entry : aspectData.getEffect().entrySet()) {
                if (entry.getValue() instanceof Map) {
                    @SuppressWarnings("unchecked")
                    Map<String, Object> range = (Map<String, Object>) entry.getValue();
                    if (range.containsKey("min") && range.containsKey("max")) {
                        double min = ((Number) range.get("min")).doubleValue();
                        double max = ((Number) range.get("max")).doubleValue();
                        double rolled = min + Math.random() * (max - min);
                        instance.withValue(entry.getKey(), rolled);
                    }
                }
            }
        }

        AspectStorage storage = aspectManager.getStorage(player.getUniqueId());
        storage.addAspect(instance);

        player.sendMessage(PREFIX.append(Component.text(aspectData.getName() + " 위상이 추가되었습니다.", NamedTextColor.GREEN)));
    }

    private void handleClear(Player player) {
        AspectManager aspectManager = items.getAspectManager();
        AspectStorage storage = aspectManager.getStorage(player.getUniqueId());
        int count = storage.getAspectCount();
        storage.clear();

        player.sendMessage(PREFIX.append(Component.text(count + "개의 위상이 제거되었습니다.", NamedTextColor.YELLOW)));
    }

    private void handleInfo(Player player, String[] args) {
        if (args.length < 2) {
            player.sendMessage(PREFIX.append(Component.text("사용법: /aspecttest info <aspectId>")));
            return;
        }

        String aspectId = args[1];
        AspectData aspectData = plugin.getCoreModule().getDataRepository().getAspect(aspectId);
        if (aspectData == null) {
            player.sendMessage(PREFIX.append(Component.text("존재하지 않는 위상: " + aspectId, NamedTextColor.RED)));
            return;
        }

        player.sendMessage(PREFIX.append(Component.text("=== " + aspectData.getName() + " ===", NamedTextColor.AQUA)));
        player.sendMessage(Component.text("  ID: ", NamedTextColor.GRAY).append(Component.text(aspectData.getId())));
        player.sendMessage(
                Component.text("  카테고리: ", NamedTextColor.GRAY).append(Component.text(aspectData.getCategory())));
        player.sendMessage(
                Component.text("  설명: ", NamedTextColor.GRAY).append(Component.text(aspectData.getDescription())));
        if (aspectData.getAllowedSlots() != null) {
            player.sendMessage(Component.text("  허용 슬롯: ", NamedTextColor.GRAY)
                    .append(Component.text(String.join(", ", aspectData.getAllowedSlots()))));
        }
        if (aspectData.getClassRestriction() != null) {
            player.sendMessage(Component.text("  직업 제한: ", NamedTextColor.GRAY)
                    .append(Component.text(aspectData.getClassRestriction())));
        }
    }

    private void sendHelp(Player player) {
        player.sendMessage(PREFIX.append(Component.text("=== Aspect Test 명령어 ===", NamedTextColor.AQUA)));
        player.sendMessage(Component.text("/aspecttest list", NamedTextColor.GRAY)
                .append(Component.text(" - 보유 위상 목록", NamedTextColor.WHITE)));
        player.sendMessage(Component.text("/aspecttest add <id>", NamedTextColor.GRAY)
                .append(Component.text(" - 위상 추가 (테스트)", NamedTextColor.WHITE)));
        player.sendMessage(Component.text("/aspecttest info <id>", NamedTextColor.GRAY)
                .append(Component.text(" - 위상 정보", NamedTextColor.WHITE)));
        player.sendMessage(Component.text("/aspecttest clear", NamedTextColor.GRAY)
                .append(Component.text(" - 모든 위상 제거", NamedTextColor.WHITE)));
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            return filterCompletions(args[0], "list", "add", "info", "clear");
        }
        if (args.length == 2 && (args[0].equalsIgnoreCase("add") || args[0].equalsIgnoreCase("info"))) {
            // 위상 ID 자동완성
            List<String> ids = new ArrayList<>();
            for (AspectData aspect : plugin.getCoreModule().getDataRepository().getAllAspects()) {
                ids.add(aspect.getId());
            }
            return filterCompletions(args[1], ids.toArray(new String[0]));
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
