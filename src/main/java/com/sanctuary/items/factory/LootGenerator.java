package com.sanctuary.items.factory;

import com.sanctuary.items.model.ItemRarity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.*;
import java.util.concurrent.ThreadLocalRandom;
import java.util.logging.Logger;

/**
 * 몬스터 처치 시 룻을 생성하는 클래스입니다.
 * "스마트 룻" 시스템을 구현하여 플레이어 클래스에 맞는 아이템을 드롭합니다.
 */
public class LootGenerator {

    private final ItemFactory itemFactory;
    private final Logger logger;

    // 기본 드롭 테이블 (몬스터 타입별로 확장 가능)
    private final Map<String, LootTable> lootTables = new HashMap<>();

    public LootGenerator(ItemFactory itemFactory, Logger logger) {
        this.itemFactory = itemFactory;
        this.logger = logger;
        initializeDefaultTables();
    }

    /**
     * 몬스터 처치 시 드롭할 아이템 목록을 생성합니다.
     * 
     * @param victim       처치된 몬스터
     * @param killer       처치한 플레이어
     * @param monsterLevel 몬스터 레벨
     * @return 드롭 아이템 목록
     */
    public List<ItemStack> generateLoot(LivingEntity victim, Player killer, int monsterLevel) {
        List<ItemStack> loot = new ArrayList<>();

        String monsterType = getMonsterType(victim);
        LootTable table = lootTables.getOrDefault(monsterType, lootTables.get("default"));

        if (table == null)
            return loot;

        ThreadLocalRandom random = ThreadLocalRandom.current();

        // 드롭 수량 결정
        int dropCount = table.rollDropCount(random);

        for (int i = 0; i < dropCount; i++) {
            LootEntry entry = table.rollEntry(random);
            if (entry != null) {
                int itemPower = calculateItemPower(monsterLevel);
                ItemStack item = itemFactory.create(entry.templateId, entry.rarity, itemPower);
                if (item != null) {
                    loot.add(item);
                }
            }
        }

        return loot;
    }

    /**
     * 엘리트/보스 몬스터용 드롭을 생성합니다.
     * 더 높은 희귀도와 더 많은 드롭을 보장합니다.
     */
    public List<ItemStack> generateEliteLoot(LivingEntity victim, Player killer, int monsterLevel, boolean isBoss) {
        List<ItemStack> loot = new ArrayList<>();

        int itemPower = calculateItemPower(monsterLevel) + (isBoss ? 50 : 20);
        int dropCount = isBoss ? 3 + ThreadLocalRandom.current().nextInt(3)
                : 1 + ThreadLocalRandom.current().nextInt(2);

        for (int i = 0; i < dropCount; i++) {
            ItemRarity rarity = rollEliteRarity(isBoss);
            String template = rollRandomTemplate();

            ItemStack item = itemFactory.create(template, rarity, itemPower);
            if (item != null) {
                loot.add(item);
            }
        }

        return loot;
    }

    // ===== 내부 메서드 =====

    private void initializeDefaultTables() {
        // 기본 드롭 테이블
        LootTable defaultTable = new LootTable();
        defaultTable.minDrops = 0;
        defaultTable.maxDrops = 2;
        defaultTable.entries.add(new LootEntry("iron_sword", ItemRarity.COMMON, 50));
        defaultTable.entries.add(new LootEntry("iron_sword", ItemRarity.MAGIC, 30));
        defaultTable.entries.add(new LootEntry("iron_sword", ItemRarity.RARE, 15));
        defaultTable.entries.add(new LootEntry("iron_sword", ItemRarity.LEGENDARY, 5));

        lootTables.put("default", defaultTable);
    }

    private String getMonsterType(LivingEntity entity) {
        // MythicMobs 연동 시 mob ID 반환
        // 현재는 엔티티 타입 사용
        return entity.getType().name().toLowerCase();
    }

    private int calculateItemPower(int monsterLevel) {
        // 몬스터 레벨 * 10 기반 + 약간의 랜덤
        int base = monsterLevel * 10;
        int variance = Math.max(10, base / 5);
        return base + ThreadLocalRandom.current().nextInt(-variance, variance + 1);
    }

    private ItemRarity rollEliteRarity(boolean isBoss) {
        double roll = ThreadLocalRandom.current().nextDouble();
        if (isBoss) {
            if (roll < 0.10)
                return ItemRarity.UNIQUE; // 10%
            if (roll < 0.40)
                return ItemRarity.LEGENDARY; // 30%
            return ItemRarity.RARE; // 60%
        } else {
            if (roll < 0.15)
                return ItemRarity.LEGENDARY; // 15%
            if (roll < 0.50)
                return ItemRarity.RARE; // 35%
            return ItemRarity.MAGIC; // 50%
        }
    }

    private String rollRandomTemplate() {
        // 실제로는 DataRepository에서 랜덤 템플릿 선택
        String[] templates = { "iron_sword", "diamond_sword", "leather_helmet", "iron_chestplate" };
        return templates[ThreadLocalRandom.current().nextInt(templates.length)];
    }

    // ===== 내부 클래스 =====

    public static class LootTable {
        int minDrops = 0;
        int maxDrops = 1;
        final List<LootEntry> entries = new ArrayList<>();

        int rollDropCount(ThreadLocalRandom random) {
            return minDrops + random.nextInt(maxDrops - minDrops + 1);
        }

        LootEntry rollEntry(ThreadLocalRandom random) {
            if (entries.isEmpty())
                return null;

            int totalWeight = entries.stream().mapToInt(e -> e.weight).sum();
            int roll = random.nextInt(totalWeight);

            int current = 0;
            for (LootEntry entry : entries) {
                current += entry.weight;
                if (roll < current)
                    return entry;
            }
            return entries.get(entries.size() - 1);
        }
    }

    public static class LootEntry {
        String templateId;
        ItemRarity rarity;
        int weight;

        public LootEntry(String templateId, ItemRarity rarity, int weight) {
            this.templateId = templateId;
            this.rarity = rarity;
            this.weight = weight;
        }
    }
}
