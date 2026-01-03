package com.sanctuary.items.serializer;

import com.sanctuary.items.model.AffixInstance;
import com.sanctuary.items.model.ItemRarity;
import com.sanctuary.items.model.MasterworkingData;
import com.sanctuary.items.model.RpgItemData;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;

/**
 * RpgItemData를 기반으로 아이템의 Lore를 생성하는 클래스입니다.
 * 디아블로 IV 스타일의 색상과 포맷을 적용합니다.
 */
public class LoreGenerator {

    private static final NamedTextColor COLOR_GRAY = NamedTextColor.GRAY;
    private static final NamedTextColor COLOR_WHITE = NamedTextColor.WHITE;
    private static final NamedTextColor COLOR_GREEN = NamedTextColor.GREEN;
    private static final NamedTextColor COLOR_BLUE = NamedTextColor.BLUE;
    private static final NamedTextColor COLOR_YELLOW = NamedTextColor.YELLOW;
    private static final NamedTextColor COLOR_ORANGE = NamedTextColor.GOLD;
    private static final NamedTextColor COLOR_RED = NamedTextColor.RED;
    private static final NamedTextColor COLOR_PURPLE = NamedTextColor.LIGHT_PURPLE;

    /**
     * ItemStack에 RpgItemData 기반의 Lore를 적용합니다.
     */
    public ItemStack applyLore(ItemStack item, RpgItemData data) {
        if (item == null || data == null)
            return item;

        ItemMeta meta = item.getItemMeta();
        if (meta == null)
            return item;

        List<Component> lore = generateLore(data);
        meta.lore(lore);

        // 아이템 이름 설정
        Component displayName = generateDisplayName(data);
        meta.displayName(displayName);

        item.setItemMeta(meta);
        return item;
    }

    /**
     * Lore 컴포넌트 목록을 생성합니다.
     */
    public List<Component> generateLore(RpgItemData data) {
        List<Component> lore = new ArrayList<>();

        // 희귀도 및 아이템 위력
        lore.add(Component.text(data.getRarity().getDisplayName(), getRarityColor(data.getRarity()))
                .append(Component.text(" 아이템", COLOR_GRAY)));
        lore.add(Component.text("아이템 위력 " + data.getItemPower(), COLOR_GRAY));
        lore.add(Component.empty());

        // 암시적 어픽스 (회색 구분선)
        if (!data.getImplicitAffixes().isEmpty()) {
            for (AffixInstance affix : data.getImplicitAffixes()) {
                lore.add(formatAffix(affix, data.getMasterworking(), -1));
            }
            lore.add(Component.text("────────────", COLOR_GRAY));
        }

        // 명시적 어픽스
        int index = 0;
        for (AffixInstance affix : data.getExplicitAffixes()) {
            lore.add(formatAffix(affix, data.getMasterworking(), index++));
        }

        // 담금질 어픽스 (파란색 하이라이트)
        if (data.getTempering() != null) {
            if (data.getTempering().getSlot1() != null) {
                lore.add(formatTemperingAffix(data.getTempering().getSlot1()));
            }
            if (data.getTempering().getSlot2() != null) {
                lore.add(formatTemperingAffix(data.getTempering().getSlot2()));
            }
        }

        // 위상 (주황색)
        if (data.hasAspect()) {
            lore.add(Component.empty());
            lore.add(Component.text("◆ ", COLOR_ORANGE)
                    .append(Component.text(data.getAspectId(), COLOR_ORANGE)));
        }

        // 명품화 상태
        if (data.getMasterworking() != null && data.getMasterworking().getRank() > 0) {
            lore.add(Component.empty());
            String rankText = "명품화 " + data.getMasterworking().getRank() + "/12";
            lore.add(Component.text(rankText, NamedTextColor.AQUA));
        }

        // 담금질 내구도
        if (data.getTempering() != null && data.isLegendaryOrHigher()) {
            String durText = "담금질 " + data.getTempering().getDurability() + "/"
                    + data.getTempering().getMaxDurability();
            if (data.getTempering().isBricked()) {
                lore.add(Component.text(durText + " (벽돌)", COLOR_RED));
            } else {
                lore.add(Component.text(durText, COLOR_GRAY));
            }
        }

        // 요구 레벨
        if (data.getRequiredLevel() > 0) {
            lore.add(Component.empty());
            lore.add(Component.text("요구 레벨: " + data.getRequiredLevel(), COLOR_GRAY));
        }

        return lore;
    }

    /**
     * 아이템 표시 이름을 생성합니다.
     */
    public Component generateDisplayName(RpgItemData data) {
        String name = data.getDisplayName() != null ? data.getDisplayName() : data.getTemplateId();
        NamedTextColor color = getRarityColor(data.getRarity());

        Component displayName = Component.text(name, color)
                .decoration(TextDecoration.ITALIC, false);

        // GA 개수 표시
        int gaCount = data.getGreaterAffixCount();
        if (gaCount > 0) {
            displayName = displayName.append(Component.text(" ★".repeat(gaCount), COLOR_RED));
        }

        return displayName;
    }

    /**
     * 어픽스를 포맷합니다.
     */
    private Component formatAffix(AffixInstance affix, MasterworkingData mw, int index) {
        String valueStr = formatValue(affix);
        NamedTextColor valueColor = COLOR_WHITE;

        // GA는 빨간색
        if (affix.isGreater()) {
            valueColor = COLOR_RED;
        }

        // 명품화 크리티컬 적중 시 색상 변경
        if (mw != null && index >= 0) {
            int critCount = mw.getCriticalCount(index);
            if (critCount == 1)
                valueColor = COLOR_BLUE;
            else if (critCount == 2)
                valueColor = COLOR_YELLOW;
            else if (critCount >= 3)
                valueColor = COLOR_ORANGE;
        }

        return Component.text("+" + valueStr + " ", valueColor)
                .append(Component.text(getStatDisplayName(affix.getStatKey()), COLOR_GRAY));
    }

    /**
     * 담금질 어픽스를 포맷합니다 (청록색 하이라이트).
     */
    private Component formatTemperingAffix(AffixInstance affix) {
        String valueStr = formatValue(affix);
        return Component.text("◇ +" + valueStr + " ", NamedTextColor.AQUA)
                .append(Component.text(getStatDisplayName(affix.getStatKey()), COLOR_GRAY));
    }

    /**
     * 수치를 포맷합니다 (퍼센트 처리 등).
     */
    private String formatValue(AffixInstance affix) {
        double value = affix.getValue();
        String key = affix.getStatKey();

        // 퍼센트 스탯 처리
        if (isPercentStat(key)) {
            return String.format("%.1f%%", value * 100);
        }

        // 정수 표시
        if (value == Math.floor(value)) {
            return String.valueOf((int) value);
        }

        return String.format("%.1f", value);
    }

    private boolean isPercentStat(String key) {
        return key.contains("CHANCE") || key.contains("PERCENT") ||
                key.contains("REDUCTION") || key.contains("SPEED") ||
                key.equals("CRIT_DAMAGE") || key.equals("VULNERABLE_DAMAGE");
    }

    private String getStatDisplayName(String key) {
        // 간단한 변환 (실제로는 DataRepository에서 조회)
        return switch (key) {
            case "WEAPON_DAMAGE" -> "무기 데미지";
            case "CRIT_CHANCE" -> "치명타 확률";
            case "CRIT_DAMAGE" -> "치명타 피해";
            case "ATTACK_SPEED" -> "공격 속도";
            case "MAX_HP" -> "최대 생명력";
            case "ARMOR" -> "방어력";
            case "STRENGTH" -> "힘";
            case "DEXTERITY" -> "민첩";
            case "INTELLIGENCE" -> "지능";
            case "WILLPOWER" -> "의지력";
            default -> key;
        };
    }

    private NamedTextColor getRarityColor(ItemRarity rarity) {
        return switch (rarity) {
            case COMMON -> COLOR_WHITE;
            case MAGIC -> COLOR_BLUE;
            case RARE -> COLOR_YELLOW;
            case LEGENDARY -> COLOR_ORANGE;
            case UNIQUE -> NamedTextColor.DARK_RED;
            case MYTHIC -> COLOR_PURPLE;
        };
    }
}
