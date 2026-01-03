package com.sanctuary.items.listener;

import com.sanctuary.combat.stat.AttributeContainer;
import com.sanctuary.combat.stat.Stat;
import com.sanctuary.core.SanctuaryCore;
import com.sanctuary.items.SanctuaryItems;
import com.sanctuary.items.model.AffixInstance;
import com.sanctuary.items.model.RpgItemData;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerItemHeldEvent;
import org.bukkit.inventory.ItemStack;

import java.util.List;

/**
 * 아이템 장착/해제 시 스탯을 적용하는 리스너입니다.
 */
public class ItemEquipListener implements Listener {

    private final SanctuaryItems items;
    private final SanctuaryCore core;

    public ItemEquipListener(SanctuaryItems items, SanctuaryCore core) {
        this.items = items;
        this.core = core;
    }

    /**
     * 플레이어가 들고 있는 아이템이 변경될 때 호출됩니다.
     * 무기 스탯을 갱신합니다.
     */
    @EventHandler(priority = EventPriority.NORMAL)
    public void onPlayerHoldChange(PlayerItemHeldEvent event) {
        Player player = event.getPlayer();

        // 새로 들 아이템
        ItemStack newItem = player.getInventory().getItem(event.getNewSlot());

        if (newItem == null || newItem.getType().isAir()) {
            // 빈손 - 기본 무기 스탯 적용
            applyDefaultWeaponStats(player);
            return;
        }

        // Sanctuary 아이템인지 확인
        RpgItemData data = items.getSerializer().read(newItem);
        if (data == null) {
            // 바닐라 아이템
            applyDefaultWeaponStats(player);
            return;
        }

        // 아이템 스탯 적용
        applyItemStats(player, data);
    }

    /**
     * 아이템 스탯을 플레이어에게 적용합니다.
     */
    private void applyItemStats(Player player, RpgItemData itemData) {
        // 현재는 StatManager에 직접 적용
        // TODO: SanctuaryCore의 EntityManager/AttributeComponent 연동

        List<AffixInstance> affixes = itemData.getAllAffixes();

        for (AffixInstance affix : affixes) {
            String statKey = affix.getStatKey();
            double value = affix.getValue();

            // 명품화 보너스 적용
            if (itemData.getMasterworking() != null) {
                int rank = itemData.getMasterworking().getRank();
                // 랭크당 5% 증가 (최대 60%)
                value *= (1.0 + rank * 0.05);
            }

            // 스탯 로그 (디버그)
            items.getPlugin().getLogger().fine(
                    "[ItemEquip] " + player.getName() + ": +" + value + " " + statKey);
        }
    }

    /**
     * 기본 무기 스탯을 적용합니다 (빈손/바닐라 아이템).
     */
    private void applyDefaultWeaponStats(Player player) {
        // 기본 주먹 스탯
        // TODO: DiabloPlugin.getCombatModule().getStatManager() 연동
    }

    /**
     * 플레이어의 전체 장비를 기반으로 스탯을 재계산합니다.
     * 
     * @param player 대상 플레이어
     */
    public void recalculateAllStats(Player player) {
        // TODO: 전체 장비 스캔 후 스탯 재계산
        // - 무기 (메인핸드/오프핸드)
        // - 방어구 (헬멧, 흉갑, 레깅스, 부츠)
        // - 장신구 (미래 구현)
    }
}
