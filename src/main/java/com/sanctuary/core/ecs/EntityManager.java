package com.sanctuary.core.ecs;

import org.bukkit.entity.Entity;
import java.util.Collection;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;

/**
 * 모든 SanctuaryEntity의 생명주기를 관리하는 매니저입니다.
 * UUID를 키로 하여 엔티티를 추적하고 관리합니다.
 * 
 * 스레드 안전성을 위해 ConcurrentHashMap을 사용합니다.
 */
public class EntityManager {

    private final Map<UUID, SanctuaryEntity> entityMap = new ConcurrentHashMap<>();
    private final Logger logger;

    public EntityManager(Logger logger) {
        this.logger = logger;
    }

    /**
     * Bukkit 엔티티에 대응하는 SanctuaryEntity를 조회하거나 생성합니다.
     * 
     * @param entity Bukkit 엔티티
     * @return SanctuaryEntity
     */
    public SanctuaryEntity getOrCreate(Entity entity) {
        if (entity == null) {
            throw new IllegalArgumentException("엔티티는 null일 수 없습니다.");
        }
        return entityMap.computeIfAbsent(entity.getUniqueId(),
                uuid -> new SanctuaryEntity(entity));
    }

    /**
     * UUID로 SanctuaryEntity를 조회합니다.
     * 
     * @param uuid 엔티티 UUID
     * @return SanctuaryEntity 또는 null
     */
    public SanctuaryEntity get(UUID uuid) {
        return entityMap.get(uuid);
    }

    /**
     * UUID로 SanctuaryEntity를 Optional로 조회합니다.
     * 
     * @param uuid 엔티티 UUID
     * @return Optional<SanctuaryEntity>
     */
    public Optional<SanctuaryEntity> getOptional(UUID uuid) {
        return Optional.ofNullable(entityMap.get(uuid));
    }

    /**
     * Bukkit 엔티티로 SanctuaryEntity를 조회합니다.
     * 
     * @param entity Bukkit 엔티티
     * @return SanctuaryEntity 또는 null
     */
    public SanctuaryEntity get(Entity entity) {
        if (entity == null)
            return null;
        return entityMap.get(entity.getUniqueId());
    }

    /**
     * 엔티티가 관리되고 있는지 확인합니다.
     * 
     * @param uuid 엔티티 UUID
     * @return 존재 여부
     */
    public boolean contains(UUID uuid) {
        return entityMap.containsKey(uuid);
    }

    /**
     * 엔티티를 매니저에서 제거합니다.
     * 
     * @param uuid 제거할 엔티티 UUID
     * @return 제거된 SanctuaryEntity 또는 null
     */
    public SanctuaryEntity remove(UUID uuid) {
        SanctuaryEntity removed = entityMap.remove(uuid);
        if (removed != null) {
            removed.getComponents().clear();
        }
        return removed;
    }

    /**
     * Bukkit 엔티티를 매니저에서 제거합니다.
     * 
     * @param entity 제거할 Bukkit 엔티티
     * @return 제거된 SanctuaryEntity 또는 null
     */
    public SanctuaryEntity remove(Entity entity) {
        if (entity == null)
            return null;
        return remove(entity.getUniqueId());
    }

    /**
     * 유효하지 않은 엔티티들을 정리합니다.
     * 주기적으로 호출하여 메모리 누수를 방지합니다.
     * 
     * @return 정리된 엔티티 수
     */
    public int cleanup() {
        int removed = 0;
        var iterator = entityMap.entrySet().iterator();
        while (iterator.hasNext()) {
            var entry = iterator.next();
            if (!entry.getValue().isValid()) {
                entry.getValue().getComponents().clear();
                iterator.remove();
                removed++;
            }
        }
        if (removed > 0) {
            logger.fine("[EntityManager] 유효하지 않은 엔티티 " + removed + "개 정리됨.");
        }
        return removed;
    }

    /**
     * 모든 관리 중인 엔티티를 반환합니다.
     * 
     * @return SanctuaryEntity 컬렉션
     */
    public Collection<SanctuaryEntity> getAll() {
        return entityMap.values();
    }

    /**
     * 관리 중인 엔티티 수를 반환합니다.
     * 
     * @return 엔티티 수
     */
    public int size() {
        return entityMap.size();
    }

    /**
     * 모든 엔티티를 제거합니다.
     */
    public void clear() {
        for (SanctuaryEntity entity : entityMap.values()) {
            entity.getComponents().clear();
        }
        entityMap.clear();
        logger.info("[EntityManager] 모든 엔티티 정리됨.");
    }
}
