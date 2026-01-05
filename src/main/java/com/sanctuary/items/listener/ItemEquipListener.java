package com.sanctuary.items.listener;

import com.sanctuary.combat.stat.AttributeContainer;
import com.sanctuary.combat.stat.Stat;
import com.sanctuary.core.SanctuaryCore;
import com.sanctuary.core.script.ScriptEngine;
import com.sanctuary.items.SanctuaryItems;
import com.sanctuary.items.model.AffixInstance;
import com.sanctuary.items.model.RpgItemData;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerItemHeldEvent;
import org.bukkit.inventory.ItemStack;
import org.luaj.vm2.LuaTable;
import org.luaj.vm2.LuaValue;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * 아이템 장착/해제 시 스탯을 적용하고 유니크 스크립트를 실행하는 리스너입니다.
 */
public class ItemEquipListener implements Listener {

    private final SanctuaryItems items;
    private final SanctuaryCore core;

    // 플레이어별 장착 중인 유니크 아이템 추적
    private final Map<UUID, RpgItemData> equippedUniqueWeapons = new HashMap<>();

    public ItemEquipListener(SanctuaryItems items, SanctuaryCore core) {
        this.items = items;
        this.core = core;
    }

    /**
     * 플레이어가 들고 있는 아이템이 변경될 때 호출됩니다.
     * 무기 스탯을 갱신하고 유니크 스크립트를 실행합니다.
     */
    @EventHandler(priority = EventPriority.NORMAL)
    public void onPlayerHoldChange(PlayerItemHeldEvent event) {
        Player player = event.getPlayer();
        UUID playerId = player.getUniqueId();

        // 이전 아이템 해제 처리
        ItemStack oldItem = player.getInventory().getItem(event.getPreviousSlot());
        if (oldItem != null && !oldItem.getType().isAir()) {
            RpgItemData oldData = items.getSerializer().read(oldItem);
            if (oldData != null && oldData.hasUniqueScript()) {
                executeUnequipScript(player, oldData);
            }
        }
        equippedUniqueWeapons.remove(playerId);

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

        // 유니크 스크립트 실행
        if (data.hasUniqueScript()) {
            executeEquipScript(player, data);
            equippedUniqueWeapons.put(playerId, data);
        }
    }

    /**
     * 아이템 스탯을 플레이어에게 적용합니다.
     */
    private void applyItemStats(Player player, RpgItemData itemData) {
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
     * 유니크 아이템 장착 스크립트를 실행합니다.
     */
    private void executeEquipScript(Player player, RpgItemData itemData) {
        String scriptName = itemData.getOnEquipScript();
        if (scriptName == null || scriptName.isBlank()) {
            // uniqueEffectId로 폴백
            String effectId = itemData.getUniqueEffectId();
            if (effectId != null && !effectId.isBlank()) {
                scriptName = effectId + "_on_equip";
            } else {
                return;
            }
        }

        try {
            ScriptEngine scriptEngine = core.getScriptEngine();
            LuaTable context = createContext(player, itemData, null, 0);
            scriptEngine.callFunction(scriptName, context);

            items.getPlugin().getLogger().fine(
                    "[Unique] " + player.getName() + " 장착: " + scriptName);
        } catch (Exception e) {
            items.getPlugin().getLogger().warning(
                    "[Unique] 장착 스크립트 실행 실패: " + scriptName + " - " + e.getMessage());
        }
    }

    /**
     * 유니크 아이템 해제 스크립트를 실행합니다.
     */
    private void executeUnequipScript(Player player, RpgItemData itemData) {
        String scriptName = itemData.getOnUnequipScript();
        if (scriptName == null || scriptName.isBlank()) {
            String effectId = itemData.getUniqueEffectId();
            if (effectId != null && !effectId.isBlank()) {
                scriptName = effectId + "_on_unequip";
            } else {
                return;
            }
        }

        try {
            ScriptEngine scriptEngine = core.getScriptEngine();
            LuaTable context = createContext(player, itemData, null, 0);
            scriptEngine.callFunction(scriptName, context);

            items.getPlugin().getLogger().fine(
                    "[Unique] " + player.getName() + " 해제: " + scriptName);
        } catch (Exception e) {
            items.getPlugin().getLogger().warning(
                    "[Unique] 해제 스크립트 실행 실패: " + scriptName + " - " + e.getMessage());
        }
    }

    /**
     * Lua 스크립트에 전달할 컨텍스트 테이블을 생성합니다.
     */
    private LuaTable createContext(Player player, RpgItemData itemData,
            org.bukkit.entity.Entity target, double damage) {
        LuaTable context = new LuaTable();

        // 플레이어 정보
        LuaTable playerTable = new LuaTable();
        playerTable.set("uuid", player.getUniqueId().toString());
        playerTable.set("name", player.getName());
        playerTable.set("health", player.getHealth());
        playerTable.set("maxHealth", player.getAttribute(
                org.bukkit.attribute.Attribute.GENERIC_MAX_HEALTH).getValue());
        context.set("player", playerTable);

        // 아이템 정보
        LuaTable itemTable = new LuaTable();
        itemTable.set("uuid", itemData.getUuid());
        itemTable.set("templateId", LuaValue.valueOf(
                itemData.getTemplateId() != null ? itemData.getTemplateId() : ""));
        itemTable.set("rarity", LuaValue.valueOf(itemData.getRarity().name()));
        itemTable.set("itemPower", itemData.getItemPower());
        if (itemData.getUniqueEffectId() != null) {
            itemTable.set("effectId", itemData.getUniqueEffectId());
        }
        context.set("itemData", itemTable);

        // 대상 정보 (있을 경우)
        if (target != null) {
            LuaTable targetTable = new LuaTable();
            targetTable.set("uuid", target.getUniqueId().toString());
            targetTable.set("type", target.getType().name());
            if (target instanceof org.bukkit.entity.LivingEntity living) {
                targetTable.set("health", living.getHealth());
                targetTable.set("maxHealth", living.getAttribute(
                        org.bukkit.attribute.Attribute.GENERIC_MAX_HEALTH).getValue());
            }
            context.set("target", targetTable);
        }

        // 피해 정보
        context.set("damage", damage);

        return context;
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
     */
    public void recalculateAllStats(Player player) {
        // TODO: 전체 장비 스캔 후 스탯 재계산
    }

    /**
     * 플레이어가 장착 중인 유니크 무기를 반환합니다.
     */
    public RpgItemData getEquippedUniqueWeapon(UUID playerId) {
        return equippedUniqueWeapons.get(playerId);
    }
}
