package com.sanctuary.combat.command;

import com.sanctuary.DiabloPlugin;
import com.sanctuary.combat.SanctuaryCombat;
import com.sanctuary.combat.stat.AttributeContainer;
import com.sanctuary.combat.stat.Stat;
import com.sanctuary.combat.status.StatusEffect;
import com.sanctuary.combat.status.StatusEffectManager;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * 전투 시스템 테스트용 명령어입니다.
 * 
 * 사용법:
 * - /combattest stats - 현재 스탯 확인
 * - /combattest setstats <스탯> <값> - 스탯 설정
 * - /combattest vulnerable <초> - 취약 상태 적용
 * - /combattest fortify <양> - 보강 적용
 * - /combattest cleardebuffs - 디버프 제거
 */
public class CombatTestCommand implements CommandExecutor, TabCompleter {

    private static final Component PREFIX = Component.text("[Combat] ", NamedTextColor.RED);

    private final DiabloPlugin plugin;
    private final SanctuaryCombat combat;

    public CombatTestCommand(DiabloPlugin plugin, SanctuaryCombat combat) {
        this.plugin = plugin;
        this.combat = combat;
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
            case "stats" -> handleStats(player);
            case "setstats" -> handleSetStats(player, args);
            case "vulnerable" -> handleVulnerable(player, args);
            case "fortify" -> handleFortify(player, args);
            case "bleeding" -> handleBleeding(player, args);
            case "cleardebuffs" -> handleClearDebuffs(player);
            case "clearall" -> handleClearAll(player);
            case "event" -> handleEvent(player);
            case "damage" -> handleDamage(player, args);
            default -> sendHelp(player);
        }

        return true;
    }

    private void handleEvent(Player player) {
        // 이벤트 버스 통계
        var eventBus = combat.getEventBus();
        if (eventBus == null) {
            player.sendMessage(PREFIX.append(Component.text("이벤트 버스가 초기화되지 않았습니다.", NamedTextColor.RED)));
            return;
        }

        player.sendMessage(PREFIX.append(Component.text("=== 이벤트 버스 상태 ===", NamedTextColor.AQUA)));

        var counts = eventBus.getEventCounts();
        if (counts.isEmpty()) {
            player.sendMessage(Component.text("  발생한 이벤트가 없습니다.", NamedTextColor.GRAY));
        } else {
            for (var entry : counts.entrySet()) {
                player.sendMessage(Component.text("  " + entry.getKey().name() + ": ", NamedTextColor.GRAY)
                        .append(Component.text(entry.getValue() + "회", NamedTextColor.WHITE)));
            }
        }

        // 리스너 수
        player.sendMessage(Component.text("  DAMAGE_DEALT 리스너: ", NamedTextColor.GRAY)
                .append(Component.text(
                        eventBus.getListenerCount(com.sanctuary.combat.event.CombatEventType.DAMAGE_DEALT) + "개",
                        NamedTextColor.WHITE)));
    }

    private void handleDamage(Player player, String[] args) {
        // 데미지 계산 테스트 (자기 자신에게)
        double skillCoeff = args.length > 1 ? Double.parseDouble(args[1]) : 1.0;

        AttributeContainer stats = combat.getStatManager().getStats(player);

        player.sendMessage(
                PREFIX.append(Component.text("=== 데미지 계산 테스트 (스킬 계수: " + skillCoeff + ") ===", NamedTextColor.AQUA)));

        // 수동 계산 시뮬레이션
        double weaponDamage = stats.getValue(Stat.WEAPON_DAMAGE);
        double baseDamage = weaponDamage * skillCoeff;
        double critChance = stats.getValue(Stat.CRIT_CHANCE);
        double critDamage = stats.getValue(Stat.CRIT_DAMAGE);

        player.sendMessage(Component.text("  무기 데미지: ", NamedTextColor.GRAY)
                .append(Component.text(String.format("%.1f", weaponDamage), NamedTextColor.WHITE)));
        player.sendMessage(Component.text("  기본 피해: ", NamedTextColor.GRAY)
                .append(Component.text(String.format("%.1f", baseDamage), NamedTextColor.WHITE)));
        player.sendMessage(Component.text("  치명타 확률: ", NamedTextColor.GRAY)
                .append(Component.text(String.format("%.1f%%", critChance * 100), NamedTextColor.WHITE)));
        player.sendMessage(Component.text("  치명타 피해: ", NamedTextColor.GRAY)
                .append(Component.text(String.format("+%.0f%%", critDamage * 100), NamedTextColor.WHITE)));

        double estimatedCrit = baseDamage * (1.5 + critDamage);
        player.sendMessage(Component.text("  예상 치명타 피해: ", NamedTextColor.YELLOW)
                .append(Component.text(String.format("%.1f", estimatedCrit), NamedTextColor.WHITE)));

        // Lua 활성화 상태
        player.sendMessage(Component.text("  Lua 계산: ", NamedTextColor.GRAY)
                .append(Component.text(combat.getDamageCalculator().isLuaEnabled() ? "활성화" : "비활성화",
                        combat.getDamageCalculator().isLuaEnabled() ? NamedTextColor.GREEN : NamedTextColor.RED)));
    }

    private void handleStats(Player player) {
        AttributeContainer stats = combat.getStatManager().getStats(player);

        player.sendMessage(PREFIX.append(Component.text("=== 현재 스탯 ===", NamedTextColor.AQUA)));
        player.sendMessage(Component.text("무기 데미지: ", NamedTextColor.GRAY)
                .append(Component.text(String.valueOf(stats.getValue(Stat.WEAPON_DAMAGE)), NamedTextColor.WHITE)));
        player.sendMessage(Component.text("치명타 확률: ", NamedTextColor.GRAY)
                .append(Component.text(String.format("%.1f%%", stats.getValue(Stat.CRIT_CHANCE) * 100),
                        NamedTextColor.WHITE)));
        player.sendMessage(Component.text("치명타 피해: ", NamedTextColor.GRAY)
                .append(Component.text(String.format("+%.0f%%", stats.getValue(Stat.CRIT_DAMAGE) * 100),
                        NamedTextColor.WHITE)));
        player.sendMessage(Component.text("방어력: ", NamedTextColor.GRAY)
                .append(Component.text(String.valueOf(stats.getValue(Stat.ARMOR)), NamedTextColor.WHITE)));
        player.sendMessage(Component.text("최대 체력: ", NamedTextColor.GRAY)
                .append(Component.text(String.valueOf(stats.getValue(Stat.MAX_HP)), NamedTextColor.WHITE)));

        // 상태 이상
        StatusEffectManager statusManager = combat.getStatusEffectManager();
        var effects = statusManager.getEffects(player);
        if (!effects.isEmpty()) {
            player.sendMessage(Component.text("상태 이상:", NamedTextColor.GRAY));
            for (var effect : effects) {
                player.sendMessage(Component.text("  ")
                        .append(Component.text(effect.getDisplayName(), NamedTextColor.YELLOW))
                        .append(Component.text(" (" + String.format("%.1f초", effect.getDurationSeconds()) + ")",
                                NamedTextColor.GRAY)));
            }
        }
    }

    private void handleSetStats(Player player, String[] args) {
        if (args.length < 3) {
            player.sendMessage(PREFIX.append(Component.text("사용법: /combattest setstats <스탯> <값>")));
            player.sendMessage(Component.text("예시: /combattest setstats WEAPON_DAMAGE 100"));
            return;
        }

        try {
            Stat stat = Stat.valueOf(args[1].toUpperCase());
            double value = Double.parseDouble(args[2]);

            AttributeContainer stats = combat.getStatManager().getStats(player);
            stats.setBase(stat, value);

            player.sendMessage(PREFIX
                    .append(Component.text(stat.name() + "을(를) " + value + "(으)로 설정했습니다.", NamedTextColor.GREEN)));
        } catch (IllegalArgumentException e) {
            player.sendMessage(PREFIX.append(Component.text("올바르지 않은 스탯 이름이거나 값입니다.", NamedTextColor.RED)));
        }
    }

    private void handleVulnerable(Player player, String[] args) {
        int seconds = args.length > 1 ? Integer.parseInt(args[1]) : 5;
        int ticks = seconds * 20;

        StatusEffect vulnerable = StatusEffect.vulnerable(ticks);
        combat.getStatusEffectManager().applyEffect(player, vulnerable);

        player.sendMessage(
                PREFIX.append(Component.text("취약 상태가 " + seconds + "초간 적용되었습니다.", NamedTextColor.LIGHT_PURPLE)));
    }

    private void handleFortify(Player player, String[] args) {
        double amount = args.length > 1 ? Double.parseDouble(args[1]) : 50.0;

        StatusEffect fortify = StatusEffect.fortify(200, amount); // 10초
        combat.getStatusEffectManager().applyEffect(player, fortify);

        player.sendMessage(PREFIX.append(Component.text("보강 " + amount + "이(가) 적용되었습니다.", NamedTextColor.DARK_GREEN)));
    }

    private void handleBleeding(Player player, String[] args) {
        double dps = args.length > 1 ? Double.parseDouble(args[1]) : 5.0;

        StatusEffect bleeding = StatusEffect.bleeding(100, dps); // 5초
        combat.getStatusEffectManager().applyEffect(player, bleeding);

        player.sendMessage(PREFIX.append(Component.text("출혈 (초당 " + dps + " 피해)이 적용되었습니다.", NamedTextColor.DARK_RED)));
    }

    private void handleClearDebuffs(Player player) {
        combat.getStatusEffectManager().getEffects(player).stream()
                .filter(e -> !e.isBuff())
                .map(StatusEffect::getId)
                .toList()
                .forEach(id -> combat.getStatusEffectManager().removeEffect(player, id));

        player.sendMessage(PREFIX.append(Component.text("모든 디버프가 제거되었습니다.", NamedTextColor.GREEN)));
    }

    private void handleClearAll(Player player) {
        combat.getStatusEffectManager().clearEffects(player);
        player.sendMessage(PREFIX.append(Component.text("모든 상태 이상이 제거되었습니다.", NamedTextColor.GREEN)));
    }

    private void sendHelp(Player player) {
        player.sendMessage(PREFIX.append(Component.text("=== Combat Test 명령어 ===", NamedTextColor.AQUA)));
        player.sendMessage(Component.text("/combattest stats", NamedTextColor.GRAY)
                .append(Component.text(" - 현재 스탯 확인", NamedTextColor.WHITE)));
        player.sendMessage(Component.text("/combattest setstats <스탯> <값>", NamedTextColor.GRAY)
                .append(Component.text(" - 스탯 설정", NamedTextColor.WHITE)));
        player.sendMessage(Component.text("/combattest vulnerable [초]", NamedTextColor.GRAY)
                .append(Component.text(" - 취약 적용", NamedTextColor.WHITE)));
        player.sendMessage(Component.text("/combattest fortify [양]", NamedTextColor.GRAY)
                .append(Component.text(" - 보강 적용", NamedTextColor.WHITE)));
        player.sendMessage(Component.text("/combattest bleeding [DPS]", NamedTextColor.GRAY)
                .append(Component.text(" - 출혈 적용", NamedTextColor.WHITE)));
        player.sendMessage(Component.text("/combattest event", NamedTextColor.YELLOW)
                .append(Component.text(" - 이벤트 버스 상태", NamedTextColor.WHITE)));
        player.sendMessage(Component.text("/combattest damage [계수]", NamedTextColor.YELLOW)
                .append(Component.text(" - 데미지 계산 테스트", NamedTextColor.WHITE)));
        player.sendMessage(Component.text("/combattest cleardebuffs", NamedTextColor.GRAY)
                .append(Component.text(" - 디버프 제거", NamedTextColor.WHITE)));
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            return filterCompletions(args[0],
                    "stats", "setstats", "vulnerable", "fortify", "bleeding", "cleardebuffs", "clearall", "event",
                    "damage");
        }
        if (args.length == 2 && args[0].equalsIgnoreCase("setstats")) {
            return filterCompletions(args[1],
                    Arrays.stream(Stat.values()).map(Stat::name).toArray(String[]::new));
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
