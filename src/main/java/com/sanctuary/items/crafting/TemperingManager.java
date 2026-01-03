package com.sanctuary.items.crafting;

import com.sanctuary.core.data.DataRepository;
import com.sanctuary.core.model.AffixData;
import com.sanctuary.items.model.AffixInstance;
import com.sanctuary.items.model.ItemRarity;
import com.sanctuary.items.model.RpgItemData;
import com.sanctuary.items.model.TemperingData;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;
import java.util.logging.Logger;

/**
 * 담금질(Tempering) 시스템을 관리합니다.
 * 전설 아이템에 새로운 속성을 부여합니다.
 */
public class TemperingManager {

    private final DataRepository dataRepository;
    private final Logger logger;

    public TemperingManager(DataRepository dataRepository, Logger logger) {
        this.dataRepository = dataRepository;
        this.logger = logger;
    }

    /**
     * 담금질 결과를 저장하는 레코드입니다.
     */
    public record TemperResult(
            boolean success,
            String message,
            AffixInstance newAffix,
            int remainingDurability) {
    }

    /**
     * 담금질이 가능한지 확인합니다.
     *
     * @param item 대상 아이템
     * @return 가능 여부
     */
    public boolean canTemper(RpgItemData item) {
        // 전설 등급 이상만 가능
        if (item.getRarity().getTier() < ItemRarity.LEGENDARY.getTier()) {
            return false;
        }

        // 담금질 데이터 확인
        TemperingData tempering = item.getTempering();
        return tempering.canTemper();
    }

    /**
     * 담금질을 시도합니다.
     *
     * @param item       대상 아이템
     * @param category   담금질 매뉴얼 카테고리 (예: "weapon", "attack", "defense")
     * @param slotNumber 슬롯 번호 (1 또는 2)
     * @return 결과
     */
    public TemperResult attemptTemper(RpgItemData item, String category, int slotNumber) {
        // 1. 검증
        if (!canTemper(item)) {
            return new TemperResult(false, "담금질이 불가능한 아이템입니다.", null, 0);
        }

        if (slotNumber != 1 && slotNumber != 2) {
            return new TemperResult(false, "잘못된 슬롯 번호입니다.", null, 0);
        }

        TemperingData tempering = item.getTempering();

        // 2. 카테고리에서 랜덤 어픽스 선택
        List<AffixData> pool = getTemperingPool(category);
        if (pool.isEmpty()) {
            return new TemperResult(false, "담금질 풀이 비어있습니다: " + category, null, 0);
        }

        AffixData selectedAffix = pool.get(ThreadLocalRandom.current().nextInt(pool.size()));

        // 3. 어픽스 인스턴스 생성
        AffixInstance newInstance = rollTemperAffix(selectedAffix, item.getItemPower());

        // 4. 슬롯에 적용
        if (slotNumber == 1) {
            tempering.setSlot1(newInstance);
        } else {
            tempering.setSlot2(newInstance);
        }

        // 5. 내구도 차감
        tempering.consumeDurability();

        logger.info("[Tempering] 담금질 성공: " + newInstance.getStatKey() + " = " +
                String.format("%.1f", newInstance.getValue()) +
                " (남은 내구도: " + tempering.getDurability() + ")");

        return new TemperResult(
                true,
                "담금질 성공!",
                newInstance,
                tempering.getDurability());
    }

    /**
     * 담금질 카테고리별 사용 가능한 어픽스 풀을 반환합니다.
     */
    private List<AffixData> getTemperingPool(String category) {
        List<AffixData> result = new ArrayList<>();

        for (AffixData affix : dataRepository.getAllAffixes()) {
            // tempering 태그가 있고 해당 카테고리에 속하는 어픽스 필터링
            if (affix.getTags() != null && affix.getTags().contains("TEMPERING")) {
                if (category.equalsIgnoreCase("ALL") ||
                        (affix.getCategory() != null && affix.getCategory().equalsIgnoreCase(category))) {
                    result.add(affix);
                }
            }
        }

        // 템퍼링 전용이 없으면 일반 어픽스 중 일부 반환
        if (result.isEmpty()) {
            for (AffixData affix : dataRepository.getAllAffixes()) {
                result.add(affix);
                if (result.size() >= 5)
                    break; // 최대 5개
            }
        }

        return result;
    }

    /**
     * 담금질 어픽스 인스턴스를 생성합니다.
     */
    private AffixInstance rollTemperAffix(AffixData affixData, int itemPower) {
        ThreadLocalRandom random = ThreadLocalRandom.current();

        // 첫 번째 스탯 사용
        var entry = affixData.getStatModifiers().entrySet().iterator().next();
        String statKey = entry.getKey();
        double baseValue = entry.getValue();

        // 아이템 위력에 따른 스케일링
        double scaleFactor = 1.0 + (itemPower / 100.0);
        double minValue = baseValue * 0.9 * scaleFactor;
        double maxValue = baseValue * 1.1 * scaleFactor;

        double rolledValue = minValue + random.nextDouble() * (maxValue - minValue);

        AffixInstance instance = new AffixInstance(affixData.getId(), statKey, rolledValue);
        instance.setMinValue(minValue);
        instance.setMaxValue(maxValue);

        return instance;
    }
}
