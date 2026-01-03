package com.sanctuary.items.factory;

import com.sanctuary.core.data.DataRepository;
import com.sanctuary.core.ecs.EntityManager;
import com.sanctuary.core.ecs.SanctuaryEntity;
import com.sanctuary.core.ecs.component.IdentityComponent;
import com.sanctuary.core.model.ItemBaseData;
import org.bukkit.entity.Player;
import java.util.*;
import java.util.concurrent.ThreadLocalRandom;
import java.util.logging.Logger;

/**
 * 스마트 룻 시스템 - 플레이어 직업에 맞는 아이템을 우선 드롭합니다.
 * 디아블로 IV의 스마트 룻 시스템을 구현합니다.
 */
public class SmartLootManager {

    private final DataRepository dataRepository;
    private final EntityManager entityManager;
    private final Logger logger;

    // 직업별 어울리는 아이템 타입 (스마트 룻 가중치 부여)
    private static final Map<String, Set<String>> CLASS_ITEM_PREFERENCES = new HashMap<>();

    // 직업에 맞는 아이템 드롭 확률 증가 배수
    private static final double SMART_LOOT_BONUS = 0.85; // 85%

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

        // 성기사 계열
        CLASS_ITEM_PREFERENCES.put("DRUID", Set.of(
                "STAFF", "TWO_HANDED_MACE", "TOTEM",
                "MEDIUM_ARMOR", "HELM", "GLOVES", "BOOTS", "PANTS"));

        // 성전사 계열
        CLASS_ITEM_PREFERENCES.put("CRUSADER", Set.of(
                "ONE_HANDED_SWORD", "ONE_HANDED_MACE", "SHIELD", "FLAIL",
                "HEAVY_ARMOR", "HELM", "GLOVES", "BOOTS", "PANTS"));
    }

    public SmartLootManager(DataRepository dataRepository, EntityManager entityManager, Logger logger) {
        this.dataRepository = dataRepository;
        this.entityManager = entityManager;
        this.logger = logger;
    }

    /**
     * 플레이어 직업을 감지합니다.
     *
     * @param player 플레이어
     * @return 직업 이름 (없으면 null)
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
     * 스마트 룻을 적용하여 아이템 템플릿 풀을 조정합니다.
     *
     * @param player   플레이어
     * @param allItems 사용 가능한 모든 아이템 템플릿
     * @return 가중치가 조정된 템플릿 목록
     */
    public List<WeightedTemplate> applySmartLoot(Player player, Collection<ItemBaseData> allItems) {
        String playerClass = detectPlayerClass(player);
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

    /**
     * 스마트 룻이 적용된 랜덤 템플릿을 선택합니다.
     *
     * @param player    플레이어
     * @param itemTypes 사용할 아이템 타입 필터 (null이면 전체)
     * @return 선택된 템플릿 ID
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
     * 가중치가 부여된 템플릿을 나타냅니다.
     */
    public record WeightedTemplate(String templateId, int weight) {
    }
}
