package com.sanctuary.items.inventory;

import com.sanctuary.items.factory.ItemFactory;
import com.sanctuary.items.model.RpgItemData;
import com.sanctuary.items.serializer.ItemSerializer;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.logging.Logger;

/**
 * 인벤토리 동기화 서비스
 * VirtualInventory와 마인크래프트 인벤토리 간 동기화를 담당합니다.
 */
public class InventorySyncService {

    private final ItemSerializer serializer;
    private final ItemFactory itemFactory;
    private final Logger logger;

    // 플레이어별 VirtualInventory 캐시
    private final Map<UUID, VirtualInventory> inventories = new HashMap<>();

    public InventorySyncService(ItemSerializer serializer, ItemFactory itemFactory, Logger logger) {
        this.serializer = serializer;
        this.itemFactory = itemFactory;
        this.logger = logger;
    }

    /**
     * 플레이어의 VirtualInventory를 가져오거나 생성합니다.
     */
    public VirtualInventory getInventory(Player player) {
        return inventories.computeIfAbsent(player.getUniqueId(),
                id -> new VirtualInventory(player, serializer, itemFactory));
    }

    /**
     * MC 인벤토리에서 VirtualInventory로 동기화합니다.
     */
    public void syncFromMinecraft(Player player) {
        VirtualInventory virtual = getInventory(player);
        PlayerInventory mc = player.getInventory();

        // 장비 슬롯 동기화
        syncEquipmentSlot(virtual, mc.getHelmet(), VirtualInventory.EquipmentSlot.HELMET);
        syncEquipmentSlot(virtual, mc.getChestplate(), VirtualInventory.EquipmentSlot.CHEST);
        syncEquipmentSlot(virtual, mc.getLeggings(), VirtualInventory.EquipmentSlot.LEGS);
        syncEquipmentSlot(virtual, mc.getBoots(), VirtualInventory.EquipmentSlot.BOOTS);
        syncEquipmentSlot(virtual, mc.getItemInMainHand(), VirtualInventory.EquipmentSlot.WEAPON);
        syncEquipmentSlot(virtual, mc.getItemInOffHand(), VirtualInventory.EquipmentSlot.OFFHAND);

        logger.fine("[InvSync] " + player.getName() + " MC→Virtual 동기화 완료");
    }

    /**
     * VirtualInventory에서 MC 인벤토리로 동기화합니다.
     */
    public void syncToMinecraft(Player player) {
        VirtualInventory virtual = getInventory(player);
        PlayerInventory mc = player.getInventory();

        // 장비 슬롯 동기화
        Map<VirtualInventory.EquipmentSlot, RpgItemData> equipped = virtual.getEquippedItems();

        // 방어구 동기화
        applyToMinecraft(mc, equipped.get(VirtualInventory.EquipmentSlot.HELMET),
                slot -> mc.setHelmet(slot));
        applyToMinecraft(mc, equipped.get(VirtualInventory.EquipmentSlot.CHEST),
                slot -> mc.setChestplate(slot));
        applyToMinecraft(mc, equipped.get(VirtualInventory.EquipmentSlot.LEGS),
                slot -> mc.setLeggings(slot));
        applyToMinecraft(mc, equipped.get(VirtualInventory.EquipmentSlot.BOOTS),
                slot -> mc.setBoots(slot));

        logger.fine("[InvSync] " + player.getName() + " Virtual→MC 동기화 완료");
    }

    private void syncEquipmentSlot(VirtualInventory virtual, ItemStack mcItem,
            VirtualInventory.EquipmentSlot slot) {
        if (mcItem == null || mcItem.getType().isAir()) {
            return;
        }

        RpgItemData data = serializer.read(mcItem);
        if (data != null) {
            virtual.getEquippedItems().put(slot, data);
        }
    }

    private void applyToMinecraft(PlayerInventory mc, RpgItemData data,
            java.util.function.Consumer<ItemStack> setter) {
        if (data == null) {
            setter.accept(null);
            return;
        }

        // TODO: ItemFactory를 통해 ItemStack 생성
        // 현재는 serializer로 직접 쓰기 불가 (읽기 전용)
    }

    /**
     * 플레이어 로그아웃 시 정리
     */
    public void cleanup(Player player) {
        inventories.remove(player.getUniqueId());
    }

    /**
     * 전체 정리
     */
    public void shutdown() {
        inventories.clear();
    }
}
