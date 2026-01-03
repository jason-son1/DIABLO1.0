package com.sanctuary.core.ecs;

import org.bukkit.entity.Entity;
import java.util.Optional;
import java.util.UUID;

/**
 * Bukkit Entity를 래핑하여 ECS 컴포넌트를 관리하는 래퍼 클래스입니다.
 * 모든 Sanctuary 시스템 로직은 이 클래스를 통해 엔티티 데이터에 접근합니다.
 */
public class SanctuaryEntity {

    private final UUID uuid;
    private final Entity bukkitEntity;
    private final ComponentContainer components;

    /**
     * SanctuaryEntity를 생성합니다.
     * 
     * @param bukkitEntity 래핑할 Bukkit 엔티티
     */
    public SanctuaryEntity(Entity bukkitEntity) {
        if (bukkitEntity == null) {
            throw new IllegalArgumentException("Bukkit 엔티티는 null일 수 없습니다.");
        }
        this.bukkitEntity = bukkitEntity;
        this.uuid = bukkitEntity.getUniqueId();
        this.components = new ComponentContainer();
    }

    /**
     * 엔티티의 UUID를 반환합니다.
     * 
     * @return UUID
     */
    public UUID getUuid() {
        return uuid;
    }

    /**
     * 래핑된 Bukkit 엔티티를 반환합니다.
     * 
     * @return Bukkit Entity
     */
    public Entity getBukkitEntity() {
        return bukkitEntity;
    }

    /**
     * 컴포넌트 컨테이너를 반환합니다.
     * 
     * @return ComponentContainer
     */
    public ComponentContainer getComponents() {
        return components;
    }

    // ===== 컴포넌트 편의 메서드 =====

    /**
     * 컴포넌트를 부착합니다.
     * 
     * @param component 부착할 컴포넌트
     * @param <T>       컴포넌트 타입
     * @return 이 엔티티 (체이닝용)
     */
    public <T extends Component> SanctuaryEntity attach(T component) {
        components.attach(component);
        return this;
    }

    /**
     * 지정된 타입의 컴포넌트를 조회합니다.
     * 
     * @param type 컴포넌트 클래스
     * @param <T>  컴포넌트 타입
     * @return 컴포넌트 또는 null
     */
    public <T extends Component> T getComponent(Class<T> type) {
        return components.get(type);
    }

    /**
     * 지정된 타입의 컴포넌트를 Optional로 조회합니다.
     * 
     * @param type 컴포넌트 클래스
     * @param <T>  컴포넌트 타입
     * @return Optional 컴포넌트
     */
    public <T extends Component> Optional<T> getComponentOptional(Class<T> type) {
        return components.getOptional(type);
    }

    /**
     * 지정된 타입의 컴포넌트가 존재하는지 확인합니다.
     * 
     * @param type 컴포넌트 클래스
     * @return 존재 여부
     */
    public boolean hasComponent(Class<? extends Component> type) {
        return components.has(type);
    }

    /**
     * 지정된 타입의 컴포넌트를 제거합니다.
     * 
     * @param type 컴포넌트 클래스
     * @param <T>  컴포넌트 타입
     * @return 제거된 컴포넌트 또는 null
     */
    public <T extends Component> T detachComponent(Class<T> type) {
        return components.detach(type);
    }

    /**
     * 엔티티가 유효한지(월드에 존재하는지) 확인합니다.
     * 
     * @return 유효 여부
     */
    public boolean isValid() {
        return bukkitEntity != null && bukkitEntity.isValid();
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null || getClass() != obj.getClass())
            return false;
        SanctuaryEntity that = (SanctuaryEntity) obj;
        return uuid.equals(that.uuid);
    }

    @Override
    public int hashCode() {
        return uuid.hashCode();
    }

    @Override
    public String toString() {
        return "SanctuaryEntity{uuid=" + uuid + ", type=" + bukkitEntity.getType() + "}";
    }
}
