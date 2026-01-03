package com.sanctuary.items.factory;

import com.sanctuary.core.data.DataRepository;
import com.sanctuary.core.model.AffixData;
import com.sanctuary.core.model.ItemBaseData;
import com.sanctuary.items.model.*;
import com.sanctuary.items.serializer.ItemSerializer;
import com.sanctuary.items.serializer.LoreGenerator;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;

import java.util.*;
import java.util.concurrent.ThreadLocalRandom;
import java.util.logging.Logger;

/**
 * RPG 아이템을 생성하는 팩토리 클래스입니다.
 * 디아블로 IV의 아이템 드롭 로직을 구현합니다.
 */
public class ItemFactory {

    private final DataRepository dataRepository;
    private final ItemSerializer serializer;
    private final LoreGenerator loreGenerator;
    private final Logger logger;

    // GA(Greater Affix) 확률 - 희귀도별
    private static final double GA_CHANCE_LEGENDARY = 0.05; // 5%
    private static final double GA_CHANCE_UNIQUE = 0.10; // 10%

    public ItemFactory(DataRepository dataRepository, ItemSerializer serializer,
            LoreGenerator loreGenerator, Logger logger) {
        this.dataRepository = dataRepository;
        this.serializer = serializer;
        this.loreGenerator = loreGenerator;
        this.logger = logger;
    }

    /**
     * 지정된 템플릿으로 아이템을 생성합니다.
     * 
     * @param templateId 베이스 아이템 ID
     * @param rarity     희귀도
     * @param itemPower  아이템 위력
     * @return 생성된 ItemStack
     */
    public ItemStack create(String templateId, ItemRarity rarity, int itemPower) {
        ItemBaseData baseData = dataRepository.getItemBase(templateId);
        if (baseData == null) {
            logger.warning("[ItemFactory] 알 수 없는 템플릿: " + templateId);
            return null;
        }

        // RpgItemData 생성
        RpgItemData data = new RpgItemData(templateId, rarity, itemPower);
        data.setDisplayName(generateDisplayName(baseData, rarity));
        data.setRequiredLevel(calculateRequiredLevel(itemPower));

        // 어픽스 롤링
        rollAffixes(data, baseData, rarity);

        // ItemStack 생성
        Material material = Material.getMaterial(baseData.getMaterial());
        if (material == null)
            material = Material.IRON_SWORD;

        ItemStack item = new ItemStack(material);

        // PDC에 데이터 저장
        serializer.write(item, data);

        // Lore 적용
        loreGenerator.applyLore(item, data);

        logger.info("[ItemFactory] 아이템 생성: " + data.getDisplayName() + " (" + rarity.getDisplayName() + ")");
        return item;
    }

    /**
     * 랜덤 희귀도와 위력으로 아이템을 생성합니다.
     */
    public ItemStack createRandom(String templateId, int baseItemPower) {
        ItemRarity rarity = rollRarity();
        int itemPower = rollItemPower(baseItemPower);
        return create(templateId, rarity, itemPower);
    }

    /**
     * 어픽스를 롤링하여 아이템에 부여합니다.
     */
    private void rollAffixes(RpgItemData data, ItemBaseData baseData, ItemRarity rarity) {
        int affixCount = rarity.getDefaultAffixCount();
        String itemType = baseData.getItemType();

        // 적용 가능한 어픽스 필터링
        List<AffixData> availableAffixes = getAvailableAffixes(itemType);
        if (availableAffixes.isEmpty()) {
            logger.warning("[ItemFactory] 사용 가능한 어픽스가 없습니다: " + itemType);
            return;
        }

        // 어픽스 선택 (중복 방지)
        Set<String> usedAffixes = new HashSet<>();
        ThreadLocalRandom random = ThreadLocalRandom.current();

        for (int i = 0; i < affixCount && !availableAffixes.isEmpty(); i++) {
            AffixData affixData = availableAffixes.get(random.nextInt(availableAffixes.size()));

            if (usedAffixes.contains(affixData.getId())) {
                i--;
                continue;
            }
            usedAffixes.add(affixData.getId());

            // 어픽스 인스턴스 생성
            AffixInstance instance = rollAffixInstance(affixData, data.getItemPower(), rarity);
            data.getExplicitAffixes().add(instance);
        }
    }

    /**
     * 단일 어픽스 인스턴스를 롤링합니다.
     */
    private AffixInstance rollAffixInstance(AffixData affixData, int itemPower, ItemRarity rarity) {
        ThreadLocalRandom random = ThreadLocalRandom.current();

        // 스탯 모디파이어에서 첫 번째 스탯 사용
        Map.Entry<String, Double> statEntry = affixData.getStatModifiers().entrySet().iterator().next();
        String statKey = statEntry.getKey();
        double baseValue = statEntry.getValue();

        // 아이템 위력에 따른 스케일링
        double scaleFactor = 1.0 + (itemPower / 100.0);
        double minValue = baseValue * 0.8 * scaleFactor;
        double maxValue = baseValue * 1.2 * scaleFactor;

        // GA 확률 계산
        double gaChance = rarity.getTier() >= ItemRarity.UNIQUE.getTier() ? GA_CHANCE_UNIQUE : GA_CHANCE_LEGENDARY;
        boolean isGA = rarity.getTier() >= ItemRarity.LEGENDARY.getTier() && random.nextDouble() < gaChance;

        AffixInstance instance;
        if (isGA) {
            instance = AffixInstance.createGreater(affixData.getId(), statKey, maxValue);
        } else {
            double rolledValue = minValue + random.nextDouble() * (maxValue - minValue);
            instance = new AffixInstance(affixData.getId(), statKey, rolledValue);
        }

        instance.setMinValue(minValue);
        instance.setMaxValue(maxValue);

        return instance;
    }

    /**
     * 아이템 타입에 맞는 어픽스 목록을 반환합니다.
     */
    private List<AffixData> getAvailableAffixes(String itemType) {
        List<AffixData> result = new ArrayList<>();
        for (AffixData affix : dataRepository.getAllAffixes()) {
            if (affix.getAllowedItemTypes() == null ||
                    affix.getAllowedItemTypes().contains(itemType) ||
                    affix.getAllowedItemTypes().contains("ALL")) {
                result.add(affix);
            }
        }
        return result;
    }

    /**
     * 희귀도를 랜덤으로 결정합니다.
     */
    private ItemRarity rollRarity() {
        double roll = ThreadLocalRandom.current().nextDouble();
        if (roll < 0.01)
            return ItemRarity.LEGENDARY; // 1%
        if (roll < 0.10)
            return ItemRarity.RARE; // 9%
        if (roll < 0.35)
            return ItemRarity.MAGIC; // 25%
        return ItemRarity.COMMON; // 65%
    }

    /**
     * 아이템 위력을 랜덤으로 결정합니다.
     */
    private int rollItemPower(int baseItemPower) {
        int variance = Math.max(5, baseItemPower / 10);
        return baseItemPower + ThreadLocalRandom.current().nextInt(-variance, variance + 1);
    }

    /**
     * 표시 이름을 생성합니다.
     */
    private String generateDisplayName(ItemBaseData baseData, ItemRarity rarity) {
        // 간단한 이름 생성 (실제로는 접두사/접미사 조합)
        return baseData.getId().replace("_", " ");
    }

    /**
     * 아이템 위력에 따른 요구 레벨을 계산합니다.
     */
    private int calculateRequiredLevel(int itemPower) {
        return Math.max(1, itemPower / 10);
    }
}
