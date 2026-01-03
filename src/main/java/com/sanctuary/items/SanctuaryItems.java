package com.sanctuary.items;

import com.sanctuary.DiabloPlugin;
import com.sanctuary.core.SanctuaryCore;
import com.sanctuary.items.command.ItemTestCommand;
import com.sanctuary.items.factory.ItemFactory;
import com.sanctuary.items.factory.LootGenerator;
import com.sanctuary.items.listener.ItemEquipListener;
import com.sanctuary.items.serializer.ItemSerializer;
import com.sanctuary.items.serializer.LoreGenerator;
import org.bukkit.command.PluginCommand;

/**
 * SanctuaryItems (뼈대)
 * 역할: 아이템 생성(RNG), NBT/PDC 직렬화, 인벤토리 데이터 모델
 */
public class SanctuaryItems {

    private final DiabloPlugin plugin;
    private final SanctuaryCore core;

    private ItemSerializer serializer;
    private LoreGenerator loreGenerator;
    private ItemFactory itemFactory;
    private LootGenerator lootGenerator;

    public SanctuaryItems(DiabloPlugin plugin, SanctuaryCore core) {
        this.plugin = plugin;
        this.core = core;
    }

    public void initialize() {
        plugin.getLogger().info("[SanctuaryItems] 초기화 중...");

        // 1. 직렬화 도구 초기화
        this.serializer = new ItemSerializer(plugin);
        this.loreGenerator = new LoreGenerator();

        // 2. 팩토리 초기화
        this.itemFactory = new ItemFactory(
                core.getDataRepository(),
                serializer,
                loreGenerator,
                plugin.getLogger());

        this.lootGenerator = new LootGenerator(itemFactory, plugin.getLogger());

        // 3. 이벤트 리스너 등록
        plugin.getServer().getPluginManager().registerEvents(
                new ItemEquipListener(this, core),
                plugin);

        // 4. 테스트 명령어 등록
        registerCommands();

        plugin.getLogger().info("[SanctuaryItems] 아이템 시스템 초기화 완료.");
    }

    private void registerCommands() {
        PluginCommand itemTestCmd = plugin.getCommand("itemtest");
        if (itemTestCmd != null) {
            ItemTestCommand handler = new ItemTestCommand(plugin, this);
            itemTestCmd.setExecutor(handler);
            itemTestCmd.setTabCompleter(handler);
        }
    }

    public void shutdown() {
        plugin.getLogger().info("[SanctuaryItems] 시스템 종료됨.");
    }

    // ===== Getters =====

    public ItemSerializer getSerializer() {
        return serializer;
    }

    public LoreGenerator getLoreGenerator() {
        return loreGenerator;
    }

    public ItemFactory getItemFactory() {
        return itemFactory;
    }

    public LootGenerator getLootGenerator() {
        return lootGenerator;
    }

    public DiabloPlugin getPlugin() {
        return plugin;
    }
}
