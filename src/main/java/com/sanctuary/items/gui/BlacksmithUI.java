package com.sanctuary.items.gui;

import com.sanctuary.items.SanctuaryItems;
import com.sanctuary.items.crafting.MasterworkingManager;
import com.sanctuary.items.crafting.TemperingManager;
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
 * 대장간(Blacksmith) GUI입니다.
 * Chest GUI 기반으로 담금질, 명품화 기능을 제공합니다.
 */
public class BlacksmithUI implements InventoryHolder, Listener {

    // GUI 슬롯 배치
    private static final int ITEM_SLOT = 20; // 대상 아이템 슬롯
    private static final int MATERIAL_SLOT = 24; // 재료 슬롯
    private static final int TEMPERING_BUTTON = 30; // 담금질 버튼
    private static final int MASTERWORK_BUTTON = 32; // 명품화 버튼
    private static final int INFO_SLOT = 22; // 정보 표시

    private final SanctuaryItems itemsModule;
    private final TemperingManager temperingManager;
    private final MasterworkingManager masterworkingManager;
    private final ItemSerializer serializer;

    // 플레이어별 열린 GUI 추적
    private final Map<UUID, Inventory> openInventories = new HashMap<>();
    private final Map<UUID, BlacksmithMode> playerModes = new HashMap<>();

    public enum BlacksmithMode {
        MENU, // 메인 메뉴
        TEMPERING, // 담금질 모드
        MASTERWORK // 명품화 모드
    }

    public BlacksmithUI(SanctuaryItems itemsModule, TemperingManager temperingManager,
            MasterworkingManager masterworkingManager, ItemSerializer serializer) {
        this.itemsModule = itemsModule;
        this.temperingManager = temperingManager;
        this.masterworkingManager = masterworkingManager;
        this.serializer = serializer;
    }

    @Override
    public Inventory getInventory() {
        return null;
    }

    /**
     * 대장간 GUI를 엽니다.
     */
    public void open(Player player) {
        Inventory gui = Bukkit.createInventory(this, 54,
                Component.text("⚒ 대장간", NamedTextColor.DARK_GRAY)
                        .decoration(TextDecoration.BOLD, true));

        // 배경 채우기
        ItemStack glass = createGlassPane(Material.GRAY_STAINED_GLASS_PANE, " ");
        for (int i = 0; i < 54; i++) {
            gui.setItem(i, glass);
        }

        // 아이템 슬롯 (빈 슬롯)
        gui.setItem(ITEM_SLOT, null);

        // 재료 슬롯 (빈 슬롯)
        gui.setItem(MATERIAL_SLOT, null);

        // 정보 표시
        gui.setItem(INFO_SLOT, createInfoItem());

        // 담금질 버튼
        gui.setItem(TEMPERING_BUTTON, createTemperingButton());

        // 명품화 버튼
        gui.setItem(MASTERWORK_BUTTON, createMasterworkButton());

        // 슬롯 테두리 표시
        ItemStack itemFrameTop = createGlassPane(Material.BLUE_STAINED_GLASS_PANE, "§b아이템을 올려놓으세요");
        gui.setItem(ITEM_SLOT - 9, itemFrameTop);
        gui.setItem(ITEM_SLOT - 1, createGlassPane(Material.BLUE_STAINED_GLASS_PANE, " "));
        gui.setItem(ITEM_SLOT + 1, createGlassPane(Material.BLUE_STAINED_GLASS_PANE, " "));
        gui.setItem(ITEM_SLOT + 9, createGlassPane(Material.BLUE_STAINED_GLASS_PANE, " "));

        ItemStack materialFrameTop = createGlassPane(Material.ORANGE_STAINED_GLASS_PANE, "§6재료를 올려놓으세요");
        gui.setItem(MATERIAL_SLOT - 9, materialFrameTop);
        gui.setItem(MATERIAL_SLOT - 1, createGlassPane(Material.ORANGE_STAINED_GLASS_PANE, " "));
        gui.setItem(MATERIAL_SLOT + 1, createGlassPane(Material.ORANGE_STAINED_GLASS_PANE, " "));
        gui.setItem(MATERIAL_SLOT + 9, createGlassPane(Material.ORANGE_STAINED_GLASS_PANE, " "));

        // 모드 설정 및 열기
        openInventories.put(player.getUniqueId(), gui);
        playerModes.put(player.getUniqueId(), BlacksmithMode.MENU);
        player.openInventory(gui);
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getInventory().getHolder() instanceof BlacksmithUI)) {
            return;
        }

        if (!(event.getWhoClicked() instanceof Player player)) {
            return;
        }

        int slot = event.getRawSlot();

        // 아이템/재료 슬롯은 아이템 배치 허용
        if (slot == ITEM_SLOT || slot == MATERIAL_SLOT) {
            // 아이템 배치 허용 (취소하지 않음)
            return;
        }

        // 그 외는 클릭 취소
        event.setCancelled(true);

        // 버튼 처리
        if (slot == TEMPERING_BUTTON) {
            handleTemperingClick(player, event.getInventory());
        } else if (slot == MASTERWORK_BUTTON) {
            handleMasterworkClick(player, event.getInventory());
        }
    }

    @EventHandler
    public void onInventoryClose(InventoryCloseEvent event) {
        if (!(event.getInventory().getHolder() instanceof BlacksmithUI)) {
            return;
        }

        if (!(event.getPlayer() instanceof Player player)) {
            return;
        }

        // 남은 아이템 반환
        Inventory inv = event.getInventory();
        ItemStack itemSlot = inv.getItem(ITEM_SLOT);
        ItemStack materialSlot = inv.getItem(MATERIAL_SLOT);

        if (itemSlot != null && itemSlot.getType() != Material.AIR) {
            player.getInventory().addItem(itemSlot);
        }
        if (materialSlot != null && materialSlot.getType() != Material.AIR) {
            player.getInventory().addItem(materialSlot);
        }

        // 추적에서 제거
        openInventories.remove(player.getUniqueId());
        playerModes.remove(player.getUniqueId());
    }

    private void handleTemperingClick(Player player, Inventory inv) {
        ItemStack targetItem = inv.getItem(ITEM_SLOT);
        if (targetItem == null || targetItem.getType() == Material.AIR) {
            player.sendMessage(Component.text("§c아이템을 올려놓으세요."));
            return;
        }

        // RpgItemData 파싱
        RpgItemData itemData = serializer.read(targetItem);
        if (itemData == null) {
            player.sendMessage(Component.text("§c이 아이템은 담금질할 수 없습니다."));
            return;
        }

        // 담금질 가능 여부 확인
        if (!temperingManager.canTemper(itemData)) {
            player.sendMessage(Component.text("§c담금질이 불가능한 아이템입니다. (전설 등급 이상 필요)"));
            return;
        }

        // 담금질 시도 (카테고리는 "ALL"로 기본)
        var result = temperingManager.attemptTemper(itemData, "ALL", 1);
        if (result.success()) {
            // 아이템 업데이트
            ItemStack updated = serializer.write(targetItem, itemData);
            inv.setItem(ITEM_SLOT, updated);
            player.sendMessage(Component.text("§a담금질 성공! " + result.newAffix().getStatKey() +
                    " +" + String.format("%.1f", result.newAffix().getValue())));
            player.playSound(player.getLocation(), org.bukkit.Sound.BLOCK_ANVIL_USE, 1.0f, 1.0f);
        } else {
            player.sendMessage(Component.text("§c" + result.message()));
        }
    }

    private void handleMasterworkClick(Player player, Inventory inv) {
        ItemStack targetItem = inv.getItem(ITEM_SLOT);
        if (targetItem == null || targetItem.getType() == Material.AIR) {
            player.sendMessage(Component.text("§c아이템을 올려놓으세요."));
            return;
        }

        // RpgItemData 파싱
        RpgItemData itemData = serializer.read(targetItem);
        if (itemData == null) {
            player.sendMessage(Component.text("§c이 아이템은 명품화할 수 없습니다."));
            return;
        }

        // 명품화 가능 여부 확인
        if (!masterworkingManager.canUpgrade(itemData)) {
            player.sendMessage(Component.text("§c명품화가 불가능한 아이템입니다. (전설 등급 이상, 최대 12랭크)"));
            return;
        }

        // 명품화 시도
        var result = masterworkingManager.attemptUpgrade(itemData);
        if (result.success()) {
            // 아이템 업데이트
            ItemStack updated = serializer.write(targetItem, itemData);
            inv.setItem(ITEM_SLOT, updated);

            String message = result.isCritical()
                    ? "§6§l크리티컬 강화! §e랭크 " + result.newRank()
                    : "§a강화 성공! 랭크 " + result.newRank();
            player.sendMessage(Component.text(message));

            if (result.isCritical()) {
                player.playSound(player.getLocation(), org.bukkit.Sound.ENTITY_PLAYER_LEVELUP, 1.0f, 1.5f);
            } else {
                player.playSound(player.getLocation(), org.bukkit.Sound.BLOCK_ANVIL_USE, 1.0f, 1.2f);
            }
        } else {
            player.sendMessage(Component.text("§c" + result.message()));
        }
    }

    // ===== 아이템 생성 헬퍼 =====

    private ItemStack createGlassPane(Material material, String name) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.displayName(Component.text(name));
            item.setItemMeta(meta);
        }
        return item;
    }

    private ItemStack createInfoItem() {
        ItemStack item = new ItemStack(Material.BOOK);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.displayName(Component.text("⚒ 대장간", NamedTextColor.GOLD)
                    .decoration(TextDecoration.BOLD, true));
            meta.lore(List.of(
                    Component.text(""),
                    Component.text("§7아이템을 강화하세요!", NamedTextColor.GRAY),
                    Component.text(""),
                    Component.text("§b담금질 §f- 새로운 속성 부여", NamedTextColor.WHITE),
                    Component.text("§6명품화 §f- 모든 속성 강화", NamedTextColor.WHITE),
                    Component.text(""),
                    Component.text("§c전설 등급 이상만 가능", NamedTextColor.RED)));
            item.setItemMeta(meta);
        }
        return item;
    }

    private ItemStack createTemperingButton() {
        ItemStack item = new ItemStack(Material.ANVIL);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.displayName(Component.text("§b§l담금질", NamedTextColor.AQUA)
                    .decoration(TextDecoration.BOLD, true));
            meta.lore(List.of(
                    Component.text(""),
                    Component.text("§7아이템에 새로운 속성을 부여합니다.", NamedTextColor.GRAY),
                    Component.text("§7최대 2개의 담금질 슬롯 사용 가능", NamedTextColor.GRAY),
                    Component.text(""),
                    Component.text("§e클릭하여 담금질", NamedTextColor.YELLOW)));
            item.setItemMeta(meta);
        }
        return item;
    }

    private ItemStack createMasterworkButton() {
        ItemStack item = new ItemStack(Material.GOLDEN_APPLE);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.displayName(Component.text("§6§l명품화", NamedTextColor.GOLD)
                    .decoration(TextDecoration.BOLD, true));
            meta.lore(List.of(
                    Component.text(""),
                    Component.text("§7모든 속성을 5% 강화합니다.", NamedTextColor.GRAY),
                    Component.text("§7매 4랭크마다 크리티컬 발생!", NamedTextColor.GRAY),
                    Component.text("§7(최대 12랭크)", NamedTextColor.GRAY),
                    Component.text(""),
                    Component.text("§e클릭하여 명품화", NamedTextColor.YELLOW)));
            item.setItemMeta(meta);
        }
        return item;
    }
}
