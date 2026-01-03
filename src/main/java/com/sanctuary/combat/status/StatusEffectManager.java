package com.sanctuary.combat.status;

import org.bukkit.entity.LivingEntity;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;
import java.util.stream.Collectors;

/**
 * 엔티티별 상태 이상을 관리하는 매니저입니다.
 * 매 틱마다 update()를 호출하여 효과 지속시간을 처리합니다.
 */
public class StatusEffectManager {

    private final Logger logger;

    // UUID -> (효과 ID -> StatusEffect)
    private final Map<UUID, Map<String, StatusEffect>> entityEffects = new ConcurrentHashMap<>();

    public StatusEffectManager(Logger logger) {
        this.logger = logger;
    }

    // ===== 효과 적용 =====

    /**
     * 엔티티에 상태 효과를 적용합니다.
     * 이미 같은 효과가 있으면 갱신/중첩됩니다.
     * 
     * @param entity 대상 엔티티
     * @param effect 적용할 효과
     */
    public void applyEffect(LivingEntity entity, StatusEffect effect) {
        if (entity == null || effect == null)
            return;

        Map<String, StatusEffect> effects = entityEffects.computeIfAbsent(
                entity.getUniqueId(), k -> new ConcurrentHashMap<>());

        StatusEffect existing = effects.get(effect.getId());
        if (existing != null) {
            // 기존 효과가 있으면 갱신/중첩
            existing.refresh(effect.getDurationTicks());
            existing.addStack();
            logger.fine("[StatusEffect] " + effect.getId() + " 갱신됨: " + existing);
        } else {
            // 새 효과 추가
            effects.put(effect.getId(), effect);
            logger.fine("[StatusEffect] " + effect.getId() + " 적용됨: " + effect);
        }
    }

    /**
     * 엔티티에서 특정 효과를 제거합니다.
     */
    public boolean removeEffect(LivingEntity entity, String effectId) {
        if (entity == null)
            return false;

        Map<String, StatusEffect> effects = entityEffects.get(entity.getUniqueId());
        if (effects != null) {
            StatusEffect removed = effects.remove(effectId.toUpperCase());
            if (removed != null) {
                logger.fine("[StatusEffect] " + effectId + " 제거됨");
                return true;
            }
        }
        return false;
    }

    /**
     * 엔티티의 모든 효과를 제거합니다.
     */
    public void clearEffects(LivingEntity entity) {
        if (entity == null)
            return;
        entityEffects.remove(entity.getUniqueId());
    }

    // ===== 효과 조회 =====

    /**
     * 엔티티가 특정 효과를 가지고 있는지 확인합니다.
     */
    public boolean hasEffect(LivingEntity entity, String effectId) {
        return getEffect(entity, effectId) != null;
    }

    /**
     * 엔티티의 특정 효과를 조회합니다.
     */
    public StatusEffect getEffect(LivingEntity entity, String effectId) {
        if (entity == null)
            return null;

        Map<String, StatusEffect> effects = entityEffects.get(entity.getUniqueId());
        if (effects == null)
            return null;

        return effects.get(effectId.toUpperCase());
    }

    /**
     * 엔티티의 모든 효과를 반환합니다.
     */
    public Collection<StatusEffect> getEffects(LivingEntity entity) {
        if (entity == null)
            return Collections.emptyList();

        Map<String, StatusEffect> effects = entityEffects.get(entity.getUniqueId());
        if (effects == null)
            return Collections.emptyList();

        return effects.values();
    }

    /**
     * 엔티티의 특정 타입의 효과들을 반환합니다.
     */
    public List<StatusEffect> getEffectsByType(LivingEntity entity, StatusType type) {
        return getEffects(entity).stream()
                .filter(e -> e.getType() == type)
                .collect(Collectors.toList());
    }

    // ===== 편의 메서드 =====

    /**
     * 엔티티가 취약(Vulnerable) 상태인지 확인합니다.
     */
    public boolean isVulnerable(LivingEntity entity) {
        return hasEffect(entity, "VULNERABLE");
    }

    /**
     * 엔티티가 보강(Fortify) 상태인지 확인합니다.
     */
    public boolean isFortified(LivingEntity entity) {
        return hasEffect(entity, "FORTIFY");
    }

    /**
     * 엔티티가 기절/동결 등 행동 불가 상태인지 확인합니다.
     */
    public boolean isIncapacitated(LivingEntity entity) {
        return hasEffect(entity, "STUNNED") || hasEffect(entity, "FROZEN");
    }

    /**
     * 엔티티가 CC(군중 제어) 상태인지 확인합니다.
     */
    public boolean hasCrowdControl(LivingEntity entity) {
        return getEffects(entity).stream()
                .anyMatch(StatusEffect::isCrowdControl);
    }

    /**
     * 보강 수치를 반환합니다.
     */
    public double getFortifyAmount(LivingEntity entity) {
        StatusEffect fortify = getEffect(entity, "FORTIFY");
        return fortify != null ? fortify.getValue() : 0.0;
    }

    // ===== 틱 업데이트 =====

    /**
     * 모든 엔티티의 효과를 업데이트합니다.
     * BukkitScheduler에서 매 틱마다 호출해야 합니다.
     */
    public void update() {
        Iterator<Map.Entry<UUID, Map<String, StatusEffect>>> entityIterator = entityEffects.entrySet().iterator();

        while (entityIterator.hasNext()) {
            Map.Entry<UUID, Map<String, StatusEffect>> entry = entityIterator.next();
            Map<String, StatusEffect> effects = entry.getValue();

            // 만료된 효과 제거
            effects.entrySet().removeIf(e -> e.getValue().tick());

            // 효과가 없으면 엔티티도 제거
            if (effects.isEmpty()) {
                entityIterator.remove();
            }
        }
    }

    /**
     * DoT(지속 피해) 효과를 처리합니다.
     * 매 20틱(1초)마다 호출하여 출혈/화상 등의 피해를 적용합니다.
     * 
     * @param entity 대상 엔티티
     * @return 적용된 DoT 피해량
     */
    public double processDoTDamage(LivingEntity entity) {
        if (entity == null)
            return 0.0;

        double totalDamage = 0.0;

        for (StatusEffect effect : getEffects(entity)) {
            String id = effect.getId();
            if ("BLEEDING".equals(id) || "BURNING".equals(id) || "POISONED".equals(id)) {
                totalDamage += effect.getTotalValue();
            }
        }

        return totalDamage;
    }

    // ===== 관리 메서드 =====

    /**
     * 유효하지 않은 엔티티의 데이터를 정리합니다.
     */
    public int cleanup(java.util.function.Predicate<UUID> isInvalid) {
        int removed = 0;
        Iterator<UUID> iterator = entityEffects.keySet().iterator();
        while (iterator.hasNext()) {
            if (isInvalid.test(iterator.next())) {
                iterator.remove();
                removed++;
            }
        }
        return removed;
    }

    /**
     * 관리 중인 엔티티 수를 반환합니다.
     */
    public int getEntityCount() {
        return entityEffects.size();
    }

    /**
     * 모든 데이터를 초기화합니다.
     */
    public void clear() {
        entityEffects.clear();
    }
}
