package com.sanctuary.items.aspect;

import com.sanctuary.core.data.DataRepository;
import com.sanctuary.core.model.AspectData;
import com.sanctuary.items.model.ItemRarity;
import com.sanctuary.items.model.RpgItemData;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadLocalRandom;
import java.util.logging.Logger;

/**
 * Aspect(위상) 시스템을 관리합니다.
 * - 전설 아이템 분해 시 위상 추출
 * - 희귀 아이템에 위상 각인 (전설 승급)
 */
public class AspectManager {

    private final DataRepository dataRepository;
    private final Logger logger;

    // 플레이어별 위상 저장소
    private final Map<UUID, AspectStorage> playerStorages;

    public AspectManager(DataRepository dataRepository, Logger logger) {
        this.dataRepository = dataRepository;
        this.logger = logger;
        this.playerStorages = new ConcurrentHashMap<>();
    }

    /**
     * 플레이어의 위상 저장소를 가져옵니다.
     */
    public AspectStorage getStorage(UUID playerId) {
        return playerStorages.computeIfAbsent(playerId, AspectStorage::new);
    }

    /**
     * 결과를 나타내는 레코드입니다.
     */
    public record AspectResult(
            boolean success,
            String message,
            AspectInstance aspect) {
    }

    /**
     * 전설 아이템에서 위상을 추출합니다.
     * 아이템은 분해됩니다.
     */
    public AspectResult extractAspect(UUID playerId, RpgItemData legendaryItem) {
        // 1. 등급 검증
        if (legendaryItem.getRarity().getTier() < ItemRarity.LEGENDARY.getTier()) {
            return new AspectResult(false, "전설 등급 이상만 위상을 추출할 수 있습니다.", null);
        }

        // 2. 아이템에 위상이 있는지 확인
        String aspectId = legendaryItem.getAspectId();
        if (aspectId == null || aspectId.isEmpty()) {
            return new AspectResult(false, "이 아이템에는 추출 가능한 위상이 없습니다.", null);
        }

        // 3. 위상 데이터 조회
        AspectData aspectData = dataRepository.getAspect(aspectId);
        if (aspectData == null) {
            return new AspectResult(false, "위상 데이터를 찾을 수 없습니다: " + aspectId, null);
        }

        // 4. 위상 인스턴스 생성 (기존 수치 또는 새로 롤링)
        AspectInstance extracted = createAspectInstance(aspectData, legendaryItem);

        // 5. 저장소에 추가
        AspectStorage storage = getStorage(playerId);
        if (!storage.addAspect(extracted)) {
            return new AspectResult(false, "위상 저장소가 가득 찼습니다.", null);
        }

        logger.info("[Aspect] 위상 추출: " + extracted.getAspectName() + " <- " + legendaryItem.getDisplayName());

        return new AspectResult(true,
                extracted.getAspectName() + " 위상을 추출했습니다.",
                extracted);
    }

    /**
     * 희귀 아이템에 위상을 각인합니다.
     * 아이템이 전설 등급으로 승급됩니다.
     */
    public AspectResult imprintAspect(UUID playerId, RpgItemData targetItem, String aspectId) {
        // 1. 대상 아이템 등급 확인 (희귀 또는 전설)
        if (targetItem.getRarity().getTier() < ItemRarity.RARE.getTier()) {
            return new AspectResult(false, "희귀 등급 이상만 위상을 각인할 수 있습니다.", null);
        }

        // 2. 저장소에서 위상 조회
        AspectStorage storage = getStorage(playerId);
        AspectInstance aspect = storage.getAspect(aspectId);
        if (aspect == null) {
            return new AspectResult(false, "보유한 위상이 없습니다: " + aspectId, null);
        }

        // 3. 슬롯 호환성 확인
        String itemSlot = targetItem.getSlotType();
        if (!aspect.canImprintToSlot(itemSlot)) {
            return new AspectResult(false,
                    "이 위상은 " + itemSlot + " 슬롯에 각인할 수 없습니다.", null);
        }

        // 4. 직업 제한 확인 (TODO: 플레이어 직업 조회)
        // if (!aspect.canUseByClass(playerClass)) { ... }

        // 5. 기존 위상 확인 (있으면 교체)
        boolean hadAspect = targetItem.getAspectId() != null;

        // 6. 위상 각인
        targetItem.setAspectId(aspect.getAspectId());
        targetItem.setAspectValues(aspect.getRolledValues());

        // 7. 희귀 → 전설 승급
        if (targetItem.getRarity() == ItemRarity.RARE) {
            targetItem.setRarity(ItemRarity.LEGENDARY);
        }

        // 8. 저장소에서 위상 제거
        storage.removeAspect(aspectId);

        String message = hadAspect
                ? aspect.getAspectName() + " 위상으로 교체되었습니다."
                : aspect.getAspectName() + " 위상이 각인되었습니다.";

        logger.info("[Aspect] 위상 각인: " + aspect.getAspectName() + " -> " + targetItem.getDisplayName());

        return new AspectResult(true, message, aspect);
    }

    /**
     * AspectData에서 AspectInstance를 생성합니다.
     */
    private AspectInstance createAspectInstance(AspectData data, RpgItemData sourceItem) {
        AspectInstance instance = new AspectInstance(
                data.getId(),
                data.getName(),
                data.getCategory(),
                data.getDescription(),
                data.getAllowedSlots(),
                data.getClassRestriction());

        // 기존 아이템에 롤링된 값이 있으면 사용
        Map<String, Double> existingValues = sourceItem.getAspectValues();
        if (existingValues != null && !existingValues.isEmpty()) {
            for (Map.Entry<String, Double> entry : existingValues.entrySet()) {
                instance.withValue(entry.getKey(), entry.getValue());
            }
        } else {
            // 새로 롤링
            Map<String, Object> effect = data.getEffect();
            if (effect != null) {
                rollAspectValues(instance, effect);
            }
        }

        instance.withSourceItem(sourceItem.getItemId());

        return instance;
    }

    /**
     * 위상 수치를 롤링합니다.
     */
    @SuppressWarnings("unchecked")
    private void rollAspectValues(AspectInstance instance, Map<String, Object> effect) {
        ThreadLocalRandom random = ThreadLocalRandom.current();

        for (Map.Entry<String, Object> entry : effect.entrySet()) {
            String key = entry.getKey();
            Object value = entry.getValue();

            if (value instanceof Map) {
                Map<String, Object> range = (Map<String, Object>) value;
                if (range.containsKey("min") && range.containsKey("max")) {
                    double min = toDouble(range.get("min"));
                    double max = toDouble(range.get("max"));
                    double rolled = min + random.nextDouble() * (max - min);
                    instance.withValue(key, rolled);
                }
            }
        }
    }

    private double toDouble(Object value) {
        if (value instanceof Number) {
            return ((Number) value).doubleValue();
        }
        return 0.0;
    }

    /**
     * 플레이어의 위상 저장소를 저장합니다.
     */
    public Map<String, Object> savePlayerStorage(UUID playerId) {
        AspectStorage storage = playerStorages.get(playerId);
        if (storage == null) {
            return null;
        }
        return storage.serialize();
    }

    /**
     * 플레이어를 정리합니다 (로그아웃 시).
     */
    public void cleanupPlayer(UUID playerId) {
        // 저장 후 제거 가능
        playerStorages.remove(playerId);
    }
}
