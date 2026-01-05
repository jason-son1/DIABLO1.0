package com.sanctuary.items.aspect;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 플레이어별 위상(Aspect) 저장소입니다.
 * 전설 아이템 분해 시 추출된 위상을 보관하고, 각인 시 사용합니다.
 */
public class AspectStorage {

    private final UUID playerId;

    // aspectId -> AspectInstance (같은 위상은 최신 것만 보관)
    private final Map<String, AspectInstance> storedAspects;

    // 위상 슬롯 제한 (디아블로 4: 무제한)
    private int maxCapacity = -1; // -1 = 무제한

    public AspectStorage(UUID playerId) {
        this.playerId = playerId;
        this.storedAspects = new ConcurrentHashMap<>();
    }

    /**
     * 위상을 저장소에 추가합니다.
     * 같은 ID의 위상이 이미 있으면 더 좋은 수치로 대체합니다.
     */
    public boolean addAspect(AspectInstance aspect) {
        // 용량 체크
        if (maxCapacity > 0 && storedAspects.size() >= maxCapacity) {
            return false;
        }

        String id = aspect.getAspectId();
        AspectInstance existing = storedAspects.get(id);

        if (existing == null) {
            storedAspects.put(id, aspect);
            return true;
        }

        // 더 좋은 수치인지 비교 (첫 번째 값 기준)
        double existingTotal = existing.getRolledValues().values().stream()
                .mapToDouble(Double::doubleValue).sum();
        double newTotal = aspect.getRolledValues().values().stream()
                .mapToDouble(Double::doubleValue).sum();

        if (newTotal > existingTotal) {
            storedAspects.put(id, aspect);
        }

        return true;
    }

    /**
     * 저장소에서 위상을 제거합니다 (각인 시 사용).
     */
    public AspectInstance removeAspect(String aspectId) {
        return storedAspects.remove(aspectId);
    }

    /**
     * 특정 위상을 조회합니다.
     */
    public AspectInstance getAspect(String aspectId) {
        return storedAspects.get(aspectId);
    }

    /**
     * 저장된 모든 위상 목록을 반환합니다.
     */
    public Collection<AspectInstance> getAllAspects() {
        return Collections.unmodifiableCollection(storedAspects.values());
    }

    /**
     * 카테고리별 위상 목록을 반환합니다.
     */
    public List<AspectInstance> getAspectsByCategory(String category) {
        List<AspectInstance> result = new ArrayList<>();
        for (AspectInstance aspect : storedAspects.values()) {
            if (aspect.getCategory().equalsIgnoreCase(category)) {
                result.add(aspect);
            }
        }
        return result;
    }

    /**
     * 특정 슬롯에 각인 가능한 위상 목록을 반환합니다.
     */
    public List<AspectInstance> getAspectsForSlot(String slotType, String playerClass) {
        List<AspectInstance> result = new ArrayList<>();
        for (AspectInstance aspect : storedAspects.values()) {
            if (aspect.canImprintToSlot(slotType) && aspect.canUseByClass(playerClass)) {
                result.add(aspect);
            }
        }
        return result;
    }

    /**
     * 위상이 저장되어 있는지 확인합니다.
     */
    public boolean hasAspect(String aspectId) {
        return storedAspects.containsKey(aspectId);
    }

    /**
     * 저장된 위상 개수를 반환합니다.
     */
    public int getAspectCount() {
        return storedAspects.size();
    }

    /**
     * 저장소를 비웁니다.
     */
    public void clear() {
        storedAspects.clear();
    }

    public UUID getPlayerId() {
        return playerId;
    }

    public int getMaxCapacity() {
        return maxCapacity;
    }

    public void setMaxCapacity(int maxCapacity) {
        this.maxCapacity = maxCapacity;
    }

    /**
     * 저장소를 Map으로 직렬화합니다 (저장용).
     */
    public Map<String, Object> serialize() {
        Map<String, Object> data = new HashMap<>();
        data.put("playerId", playerId.toString());
        data.put("maxCapacity", maxCapacity);

        List<Map<String, Object>> aspectsList = new ArrayList<>();
        for (AspectInstance aspect : storedAspects.values()) {
            Map<String, Object> aspectData = new HashMap<>();
            aspectData.put("aspectId", aspect.getAspectId());
            aspectData.put("aspectName", aspect.getAspectName());
            aspectData.put("category", aspect.getCategory());
            aspectData.put("description", aspect.getDescription());
            aspectData.put("rolledValues", aspect.getRolledValues());
            aspectData.put("sourceItemId", aspect.getSourceItemId());
            aspectData.put("extractedTime", aspect.getExtractedTime());
            aspectData.put("allowedSlots", aspect.getAllowedSlots());
            aspectData.put("classRestriction", aspect.getClassRestriction());
            aspectsList.add(aspectData);
        }
        data.put("aspects", aspectsList);

        return data;
    }

    @Override
    public String toString() {
        return String.format("AspectStorage{player=%s, count=%d}", playerId, storedAspects.size());
    }
}
