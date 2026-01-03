package com.sanctuary.core.data;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import com.sanctuary.core.model.AffixData;
import com.sanctuary.core.model.ItemBaseData;
import com.sanctuary.core.model.StatData;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.Reader;
import java.lang.reflect.Type;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

/**
 * JSON 파일로부터 데이터를 로드하는 DataRepository 구현체입니다.
 * plugins/Sanctuary/data 디렉토리를 스캔합니다.
 */
public class JsonDataLoader implements DataRepository {

    private final File dataFolder;
    private final Logger logger;
    private final Gson gson;

    private final Map<String, StatData> statMap = new HashMap<>();
    private final Map<String, AffixData> affixMap = new HashMap<>();
    private final Map<String, ItemBaseData> itemBaseMap = new HashMap<>();

    public JsonDataLoader(File pluginFolder, Logger logger) {
        this.dataFolder = new File(pluginFolder, "data");
        this.logger = logger;
        this.gson = new GsonBuilder().setPrettyPrinting().create();
    }

    @Override
    public void reload() {
        statMap.clear();
        affixMap.clear();
        itemBaseMap.clear();

        if (!dataFolder.exists()) {
            dataFolder.mkdirs();
        }

        loadStats();
        loadAffixes();
        loadItemBases();

        logger.info("[SanctuaryCore] 데이터 로드 완료. Stats: " + statMap.size() + ", Affixes: " + affixMap.size()
                + ", Items: " + itemBaseMap.size());
    }

    private void loadStats() {
        File file = new File(dataFolder, "stats.json");
        if (!file.exists()) {
            createExampleStats(file);
        }

        try (Reader reader = new FileReader(file)) {
            Type type = new TypeToken<List<StatData>>() {
            }.getType();
            List<StatData> stats = gson.fromJson(reader, type);
            if (stats != null) {
                for (StatData stat : stats) {
                    statMap.put(stat.getId(), stat);
                }
            }
        } catch (IOException e) {
            logger.severe("[SanctuaryCore] 스탯 데이터 로드 실패: " + e.getMessage());
        }
    }

    private void createExampleStats(File file) {
        try {
            java.util.List<StatData> stats = java.util.Arrays.asList(
                    new StatData("STRENGTH", "힘", "Core stat for barbarians"),
                    new StatData("DEXTERITY", "민첩", "Core stat for rogues"),
                    new StatData("WEAPON_DAMAGE", "무기 공격력", "Base weapon damage"),
                    new StatData("CRIT_CHANCE", "치명타 확률", "Chance to deal extra damage"));
            try (java.io.FileWriter writer = new java.io.FileWriter(file)) {
                gson.toJson(stats, writer);
            }
            logger.info("[SanctuaryCore] 기본 stats.json 파일이 생성되었습니다.");
        } catch (IOException e) {
            logger.severe("[SanctuaryCore] 기본 stats.json 생성 실패: " + e.getMessage());
        }
    }

    private void loadAffixes() {
        File file = new File(dataFolder, "affixes.json");
        if (!file.exists()) {
            createExampleAffixes(file);
        }

        try (Reader reader = new FileReader(file)) {
            Type type = new TypeToken<List<AffixData>>() {
            }.getType();
            List<AffixData> affixes = gson.fromJson(reader, type);
            if (affixes != null) {
                for (AffixData affix : affixes) {
                    affixMap.put(affix.getId(), affix);
                }
            }
        } catch (IOException e) {
            logger.severe("[SanctuaryCore] 어픽스 데이터 로드 실패: " + e.getMessage());
        }
    }

    private void createExampleAffixes(File file) {
        try {
            AffixData affix = new AffixData();
            affix.setId("fire_damage_plus");
            affix.getStatModifiers().put("FIRE_DAMAGE", 10.0);
            java.util.List<AffixData> affixes = java.util.Collections.singletonList(affix);
            try (java.io.FileWriter writer = new java.io.FileWriter(file)) {
                gson.toJson(affixes, writer);
            }
            logger.info("[SanctuaryCore] 기본 affixes.json 파일이 생성되었습니다.");
        } catch (IOException e) {
            logger.severe("[SanctuaryCore] 기본 affixes.json 생성 실패: " + e.getMessage());
        }
    }

    private void loadItemBases() {
        File file = new File(dataFolder, "items.json");
        if (!file.exists()) {
            createExampleItems(file);
        }

        try (Reader reader = new FileReader(file)) {
            Type type = new TypeToken<List<ItemBaseData>>() {
            }.getType();
            List<ItemBaseData> items = gson.fromJson(reader, type);
            if (items != null) {
                for (ItemBaseData item : items) {
                    itemBaseMap.put(item.getId(), item);
                }
            }
        } catch (IOException e) {
            logger.severe("[SanctuaryCore] 아이템 데이터 로드 실패: " + e.getMessage());
        }
    }

    private void createExampleItems(File file) {
        try {
            ItemBaseData item = new ItemBaseData();
            item.setId("iron_sword");
            item.setMaterial("IRON_SWORD");
            item.setItemType("SWORD");
            java.util.List<ItemBaseData> items = java.util.Collections.singletonList(item);
            try (java.io.FileWriter writer = new java.io.FileWriter(file)) {
                gson.toJson(items, writer);
            }
            logger.info("[SanctuaryCore] 기본 items.json 파일이 생성되었습니다.");
        } catch (IOException e) {
            logger.severe("[SanctuaryCore] 기본 items.json 생성 실패: " + e.getMessage());
        }
    }

    @Override
    public StatData getStat(String id) {
        return statMap.get(id);
    }

    @Override
    public Collection<StatData> getAllStats() {
        return statMap.values();
    }

    @Override
    public AffixData getAffix(String id) {
        return affixMap.get(id);
    }

    @Override
    public Collection<AffixData> getAllAffixes() {
        return affixMap.values();
    }

    @Override
    public ItemBaseData getItemBase(String id) {
        return itemBaseMap.get(id);
    }

    @Override
    public Collection<ItemBaseData> getAllItemBases() {
        return itemBaseMap.values();
    }
}
