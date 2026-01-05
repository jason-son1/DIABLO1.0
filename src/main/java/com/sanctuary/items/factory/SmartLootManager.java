package com.sanctuary.items.factory;

import com.sanctuary.core.data.DataRepository;
import com.sanctuary.core.ecs.EntityManager;
import com.sanctuary.core.ecs.SanctuaryEntity;
import com.sanctuary.core.ecs.component.IdentityComponent;
import com.sanctuary.core.model.ItemBaseData;
import com.sanctuary.core.script.ScriptEngine;
import org.bukkit.entity.Player;
import org.luaj.vm2.LuaTable;
import org.luaj.vm2.LuaValue;

import java.util.*;
import java.util.concurrent.ThreadLocalRandom;
import java.util.logging.Logger;

/**
 * 스마트 룻 시스템 - 플레이어 직업/레벨에 맞는 아이템을 우선 드롭합니다.
 * 디아블로 IV의 스마트 룻 시스템을 구현합니다.
 * 
 * 기능:
 * - 직업별 아이템 타입 가중치 (85% 스마트 룻 확률)
 * - 플레이어 레벨 기반 아이템 파워 계산
 * - 월드 티어별 보너스
 * - Lua 스크립트 오버라이드 지원
 */
public class SmartLootManager {

    private final DataRepository dataRepository;
    private final EntityManager entityManager;
    private final Logger logger;
    private ScriptEngine scriptEngine;

    // 직업별 어울리는 아이템 타입 (스마트 룻 가중치 부여)
    private static final Map<String, Set<String>> CLASS_ITEM_PREFERENCES = new HashMap<>();

    // 직업에 맞는 아이템 드롭 확률 증가 배수
    private static final double SMART_LOOT_BONUS = 0.85; // 85%

    // 레벨당 아이템 파워 증가량
    private static final int BASE_ITEM_POWER = 100;
    private static final int ITEM_POWER_PER_LEVEL = 15;

    // 월드 티어 보정 (디아블로 4)
    private static final Map<Integer, Double> WORLD_TIER_MULTIPLIERS = Map.of(
            1, 1.0, // 월드 티어 1: 어드벤처
            2, 1.25, // 월드 티어 2: 베테랑
            3, 1.5, // 월드 티어 3: 악몽
            4, 2.0 // 월드 티어 4: 고문
    );

    static {
        // 전사 계열
        CLASS_ITEM_PREFERENCES.put("BARBARIAN", Set.of(
                "TWO_HANDED_SWORD", "TWO_HANDED_AXE", "TWO_HANDED_MACE",
                "ONE_HANDED_SWORD", "ONE_HANDED_AXE", "ONE_HANDED_MACE",
                "HEAVY_ARMOR", "HELM", "GLOVES", "BOOTS", "PANTS"));

        // 도적 계열
        CLASS_ITEM_PREFERENCES.put("ROGUE", Set.of(
                "DAGGER", "SWORD", "BOW", "CROSSBOW",
                "LIGHT_ARMOR", "HELM", "GLOVES", "BOOTS", "PANTS"));

        // 마법사 계열
        CLASS_ITEM_PREFERENCES.put("SORCERER", Set.of(
                "STAFF", "WAND", "FOCUS",
                "CLOTH_ARMOR", "HELM", "GLOVES", "BOOTS", "PANTS"));

        // 강령술사 계열
        CLASS_ITEM_PREFERENCES.put("NECROMANCER", Set.of(
                "SCYTHE", "WAND", "FOCUS", "SHIELD",
                "CLOTH_ARMOR", "HELM", "GLOVES", "BOOTS", "PANTS"));

        // 드루이드 계열
        CLASS_ITEM_PREFERENCES.put("DRUID", Set.of(
                "STAFF", "TWO_HANDED_MACE", "TOTEM",
                "MEDIUM_ARMOR", "HELM", "GLOVES", "BOOTS", "PANTS"));

        // 혼령사 계열 (시즌 6)
        CLASS_ITEM_PREFERENCES.put("SPIRITBORN", Set.of(
                "GLAIVE", "QUARTERSTAFF", "FOCUS",
                "MEDIUM_ARMOR", "HELM", "GLOVES", "BOOTS", "PANTS"));
    }

    public SmartLootManager(DataRepository dataRepository, EntityManager entityManager, Logger logger) {
        this.dataRepository = dataRepository;
        this.entityManager = entityManager;
        this.logger = logger;
    }

    /**
     * Lua 스크립트 엔진을 설정합니다.
     */
    public void setScriptEngine(ScriptEngine scriptEngine) {
        this.scriptEngine = scriptEngine;
    }

    /**
     * 플레이어 직업을 감지합니다.
     */
    public String detectPlayerClass(Player player) {
        SanctuaryEntity entity = entityManager.get(player);
        if (entity == null) {
            return null;
        }

        return entity.getComponentOptional(IdentityComponent.class)
                .map(IdentityComponent::getJob)
                .orElse(null);
    }

    /**
     * 플레이어 레벨을 감지합니다.
     */
    public int detectPlayerLevel(Player player) {
        SanctuaryEntity entity = entityManager.get(player);
        if (entity == null) {
            return player.getLevel(); // 바닐라 레벨 폴백
        }

        return entity.getComponentOptional(IdentityComponent.class)
                .map(IdentityComponent::getLevel)
                .orElse(player.getLevel());
    }

    /**
     * 플레이어 월드 티어를 감지합니다.
     */
    public int detectWorldTier(Player player) {
        // TODO: 월드 티어 시스템 구현 후 연동
        // 현재는 기본 티어 1 반환
        return 1;
    }

    /**
     * 플레이어 레벨과 월드 티어에 맞는 아이템 파워를 계산합니다.
     */
    public int calculateItemPower(Player player) {
        int level = detectPlayerLevel(player);
        int worldTier = detectWorldTier(player);

        double tierMultiplier = WORLD_TIER_MULTIPLIERS.getOrDefault(worldTier, 1.0);
        int basePower = BASE_ITEM_POWER + (level * ITEM_POWER_PER_LEVEL);

        // ±10% 랜덤 변동
        double variance = 0.9 + (ThreadLocalRandom.current().nextDouble() * 0.2);

        return (int) (basePower * tierMultiplier * variance);
    }

    /**
     * 스마트 룻을 적용하여 아이템 템플릿 풀을 조정합니다.
     */
    public List<WeightedTemplate> applySmartLoot(Player player, Collection<ItemBaseData> allItems) {
        String playerClass = detectPlayerClass(player);
        int playerLevel = detectPlayerLevel(player);

        // Lua 오버라이드 체크
        if (scriptEngine != null) {
            try {
                LuaTable context = new LuaTable();
                context.set("playerUuid", player.getUniqueId().toString());
                context.set("playerClass", LuaValue.valueOf(playerClass != null ? playerClass : "NONE"));
                context.set("playerLevel", playerLevel);

                LuaValue result = scriptEngine.callFunction("smart_loot_override", context);
                if (result != null && result.istable()) {
                    // Lua에서 반환한 가중치 사용
                    return parseLuaWeights(result.checktable());
                }
            } catch (Exception e) {
                // Lua 오버라이드 실패 - 기본 로직 사용
                logger.fine("[SmartLoot] Lua 오버라이드 없음, 기본 로직 사용");
            }
        }

        if (playerClass == null) {
            // 직업이 없으면 균등 가중치
            return allItems.stream()
                    .map(item -> new WeightedTemplate(item.getId(), 100))
                    .toList();
        }

        Set<String> preferredTypes = CLASS_ITEM_PREFERENCES.getOrDefault(playerClass.toUpperCase(), Set.of());

        List<WeightedTemplate> result = new ArrayList<>();
        for (ItemBaseData item : allItems) {
            int weight = 100; // 기본 가중치

            if (preferredTypes.contains(item.getItemType())) {
                // 직업에 맞는 아이템은 가중치 증가
                weight = (int) (weight * (1.0 + SMART_LOOT_BONUS));
            } else {
                // 직업에 맞지 않는 아이템은 가중치 감소
                weight = (int) (weight * (1.0 - SMART_LOOT_BONUS * 0.5));
            }

            result.add(new WeightedTemplate(item.getId(), weight));
        }

        return result;
    }

    private List<WeightedTemplate> parseLuaWeights(LuaTable table) {
        List<WeightedTemplate> result = new ArrayList<>();
        for (LuaValue key : table.keys()) {
            LuaValue entry = table.get(key);
            if (entry.istable()) {
                String templateId = entry.get("templateId").tojstring();
                int weight = entry.get("weight").toint();
                result.add(new WeightedTemplate(templateId, weight));
            }
        }
        return result;
    }

    /**
     * 스마트 룻이 적용된 랜덤 템플릿을 선택합니다.
     */
    public String rollSmartTemplate(Player player, Set<String> itemTypes) {
        Collection<ItemBaseData> allItems = dataRepository.getAllItemBases();

        // 아이템 타입 필터링
        if (itemTypes != null && !itemTypes.isEmpty()) {
            allItems = allItems.stream()
                    .filter(item -> itemTypes.contains(item.getItemType()))
                    .toList();
        }

        if (allItems.isEmpty()) {
            logger.warning("[SmartLoot] 사용 가능한 아이템 템플릿이 없습니다.");
            return null;
        }

        List<WeightedTemplate> weighted = applySmartLoot(player, allItems);

        // 가중치 기반 선택
        int totalWeight = weighted.stream().mapToInt(w -> w.weight).sum();
        int roll = ThreadLocalRandom.current().nextInt(totalWeight);

        int current = 0;
        for (WeightedTemplate wt : weighted) {
            current += wt.weight;
            if (roll < current) {
                return wt.templateId;
            }
        }

        return weighted.get(weighted.size() - 1).templateId;
    }

    /**
     * 완전한 스마트 룻 결과를 반환합니다.
     */
    public SmartLootResult rollSmartLoot(Player player, Set<String> itemTypes) {
        String templateId = rollSmartTemplate(player, itemTypes);
        int itemPower = calculateItemPower(player);
        int playerLevel = detectPlayerLevel(player);
        String playerClass = detectPlayerClass(player);

        return new SmartLootResult(templateId, itemPower, playerLevel, playerClass);
    }

    /**
     * 가중치가 부여된 템플릿을 나타냅니다.
     */
    public record WeightedTemplate(String templateId, int weight) {
    }

    /**
     * 스마트 룻 결과를 나타냅니다.
     */
    public record SmartLootResult(
            String templateId,
            int itemPower,
            int playerLevel,
            String playerClass) {
    }
}
