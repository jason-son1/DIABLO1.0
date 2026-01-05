package com.sanctuary.items.inventory;

import com.sanctuary.items.factory.ItemFactory;
import com.sanctuary.items.model.RpgItemData;
import com.sanctuary.items.serializer.ItemSerializer;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.*;

/**
 * 가상 인벤토리 시스템 - 디아블로 스타일 그리드 인벤토리입니다.
 * 
 * 기능:
 * - 그리드 기반 아이템 배치
 * - 장비 슬롯 (5종)
 * - 아이템 정렬
 * - 바닐라 MC 인벤토리와 동기화
 */
public class VirtualInventory implements InventoryHolder, Listener {

    // GUI 구성
    private static final int ROWS = 6;
    private static final int SLOTS = ROWS * 9;

    // 장비 슬롯 위치
    private static final int WEAPON_SLOT = 0;
    private static final int OFFHAND_SLOT = 18;
    private static final int HELMET_SLOT = 2;
    private static final int CHEST_SLOT = 11;
    private static final int LEGS_SLOT = 20;
    private static final int BOOTS_SLOT = 29;
    private static final int RING1_SLOT = 4;
    private static final int RING2_SLOT = 22;
    private static final int AMULET_SLOT = 13;

    // 인벤토리 그리드 시작 위치
    private static final int GRID_START = 6;

    private final Player owner;
    private final ItemSerializer serializer;
    private final ItemFactory itemFactory;
    private final Map<Integer, RpgItemData> storedItems = new HashMap<>();

    // 장비 슬롯
    private final Map<EquipmentSlot, RpgItemData> equippedItems = new EnumMap<>(EquipmentSlot.class);

    public enum EquipmentSlot {
        WEAPON, OFFHAND, HELMET, CHEST, LEGS, BOOTS, RING1, RING2, AMULET
    }

    public VirtualInventory(Player owner, ItemSerializer serializer, ItemFactory itemFactory) {
        this.owner = owner;
        this.serializer = serializer;
        this.itemFactory = itemFactory;
    }

    @Override
    public Inventory getInventory() {
        return null;
    }

    /**
     * 인벤토리 GUI를 엽니다.
     */
    public void open() {
        Inventory gui = Bukkit.createInventory(this, SLOTS,
                Component.text("☰ 인벤토리", NamedTextColor.DARK_GRAY)
                        .decoration(TextDecoration.BOLD, true));

        // 배경 채우기
        ItemStack separator = createGlassPane(Material.BLACK_STAINED_GLASS_PANE, " ");
        for (int i = 0; i < SLOTS; i++) {
            gui.setItem(i, separator);
        }

        // 장비 슬롯 표시
        renderEquipmentSlots(gui);

        // 인벤토리 아이템 그리드 표시
        renderInventoryGrid(gui);

        owner.openInventory(gui);
    }

    private void renderEquipmentSlots(Inventory gui) {
        // 무기 슬롯
        gui.setItem(WEAPON_SLOT, createEquipSlotItem(EquipmentSlot.WEAPON, "⚔ 무기", Material.IRON_SWORD));

        // 보조 무기 슬롯
        gui.setItem(OFFHAND_SLOT, createEquipSlotItem(EquipmentSlot.OFFHAND, "🛡 보조", Material.SHIELD));

        // 방어구 슬롯
        gui.setItem(HELMET_SLOT, createEquipSlotItem(EquipmentSlot.HELMET, "🎩 투구", Material.IRON_HELMET));
        gui.setItem(CHEST_SLOT, createEquipSlotItem(EquipmentSlot.CHEST, "👕 흉갑", Material.IRON_CHESTPLATE));
        gui.setItem(LEGS_SLOT, createEquipSlotItem(EquipmentSlot.LEGS, "👖 각반", Material.IRON_LEGGINGS));
        gui.setItem(BOOTS_SLOT, createEquipSlotItem(EquipmentSlot.BOOTS, "👟 장화", Material.IRON_BOOTS));

        // 장신구 슬롯
        gui.setItem(RING1_SLOT, createEquipSlotItem(EquipmentSlot.RING1, "💍 반지 1", Material.GOLD_NUGGET));
        gui.setItem(RING2_SLOT, createEquipSlotItem(EquipmentSlot.RING2, "💍 반지 2", Material.GOLD_NUGGET));
        gui.setItem(AMULET_SLOT, createEquipSlotItem(EquipmentSlot.AMULET, "📿 목걸이", Material.EMERALD));
    }

    private ItemStack createEquipSlotItem(EquipmentSlot slot, String name, Material placeholder) {
        RpgItemData equipped = equippedItems.get(slot);

        if (equipped != null) {
            // TODO: ItemFactory를 통해 실제 아이템으로 변환
            // 현재는 placeholder 아이템에 메타 적용
            ItemStack item = new ItemStack(placeholder);
            ItemMeta meta = item.getItemMeta();
            if (meta != null) {
                meta.displayName(Component.text(equipped.getDisplayName() != null
                        ? equipped.getDisplayName()
                        : name, NamedTextColor.GOLD));
                item.setItemMeta(meta);
            }
            return item;
        }

        // 빈 슬롯 표시
        ItemStack item = new ItemStack(placeholder);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.displayName(Component.text(name, NamedTextColor.GRAY)
                    .decoration(TextDecoration.ITALIC, true));
            meta.lore(List.of(
                    Component.text(""),
                    Component.text("§7클릭하여 아이템 장착", NamedTextColor.GRAY)));
            item.setItemMeta(meta);
        }
        return item;
    }

    private void renderInventoryGrid(Inventory gui) {
        // 인벤토리 그리드에 빈 슬롯 또는 아이템 표시
        int[] gridSlots = {
                6, 7, 8,
                15, 16, 17,
                24, 25, 26,
                33, 34, 35,
                42, 43, 44,
                45, 46, 47, 48, 49, 50, 51, 52, 53
        };

        for (int i = 0; i < gridSlots.length; i++) {
            int slot = gridSlots[i];
            RpgItemData itemData = storedItems.get(i);

            if (itemData != null) {
                // ItemFactory를 통해 실제 아이템으로 변환
                ItemStack item = itemFactory.createFromData(itemData);
                gui.setItem(slot, item);
            } else {
                gui.setItem(slot, null); // 빈 슬롯
            }
        }
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getInventory().getHolder() instanceof VirtualInventory)) {
            return;
        }

        if (!(event.getWhoClicked() instanceof Player player)) {
            return;
        }

        int slot = event.getRawSlot();

        // 장비 슬롯 클릭 처리
        EquipmentSlot eqSlot = getEquipmentSlot(slot);
        if (eqSlot != null) {
            event.setCancelled(true);
            handleEquipmentClick(player, eqSlot, event.getCursor());
            return;
        }

        // 그리드 인벤토리 클릭은 허용 (아이템 이동)
    }

    private EquipmentSlot getEquipmentSlot(int slot) {
        return switch (slot) {
            case WEAPON_SLOT -> EquipmentSlot.WEAPON;
            case OFFHAND_SLOT -> EquipmentSlot.OFFHAND;
            case HELMET_SLOT -> EquipmentSlot.HELMET;
            case CHEST_SLOT -> EquipmentSlot.CHEST;
            case LEGS_SLOT -> EquipmentSlot.LEGS;
            case BOOTS_SLOT -> EquipmentSlot.BOOTS;
            case RING1_SLOT -> EquipmentSlot.RING1;
            case RING2_SLOT -> EquipmentSlot.RING2;
            case AMULET_SLOT -> EquipmentSlot.AMULET;
            default -> null;
        };
    }

    private void handleEquipmentClick(Player player, EquipmentSlot slot, ItemStack cursor) {
        if (cursor == null || cursor.getType() == Material.AIR) {
            // 장착된 아이템 해제
            RpgItemData equipped = equippedItems.remove(slot);
            if (equipped != null) {
                player.sendMessage(Component.text("§7아이템을 해제했습니다."));
            }
        } else {
            // 새 아이템 장착
            RpgItemData itemData = serializer.read(cursor);
            if (itemData != null) {
                // 기존 장착 해제 및 새 아이템 장착
                equippedItems.put(slot, itemData);
                player.sendMessage(Component.text("§a아이템을 장착했습니다."));
            } else {
                player.sendMessage(Component.text("§c이 아이템은 장착할 수 없습니다."));
            }
        }
    }

    @EventHandler
    public void onInventoryClose(InventoryCloseEvent event) {
        if (!(event.getInventory().getHolder() instanceof VirtualInventory)) {
            return;
        }
        // 인벤토리 동기화 (나중에 구현)
    }

    // ===== 유틸리티 =====

    private ItemStack createGlassPane(Material material, String name) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.displayName(Component.text(name));
            item.setItemMeta(meta);
        }
        return item;
    }

    /**
     * 인벤토리 정렬 (희귀도 기준)
     */
    public void sortByRarity() {
        List<Map.Entry<Integer, RpgItemData>> entries = new ArrayList<>(storedItems.entrySet());
        entries.sort((a, b) -> b.getValue().getRarity().ordinal() - a.getValue().getRarity().ordinal());

        storedItems.clear();
        for (int i = 0; i < entries.size(); i++) {
            storedItems.put(i, entries.get(i).getValue());
        }
    }

    /**
     * 인벤토리 정렬 (이름 기준)
     */
    public void sortByName() {
        List<Map.Entry<Integer, RpgItemData>> entries = new ArrayList<>(storedItems.entrySet());
        entries.sort((a, b) -> {
            String nameA = a.getValue().getDisplayName() != null ? a.getValue().getDisplayName() : "";
            String nameB = b.getValue().getDisplayName() != null ? b.getValue().getDisplayName() : "";
            return nameA.compareTo(nameB);
        });

        storedItems.clear();
        for (int i = 0; i < entries.size(); i++) {
            storedItems.put(i, entries.get(i).getValue());
        }
    }

    // ===== Getters =====

    public Player getOwner() {
        return owner;
    }

    public Map<EquipmentSlot, RpgItemData> getEquippedItems() {
        return Collections.unmodifiableMap(equippedItems);
    }

    public Map<Integer, RpgItemData> getStoredItems() {
        return Collections.unmodifiableMap(storedItems);
    }
}
