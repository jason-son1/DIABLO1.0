package com.sanctuary.combat.stat;

import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * StatManager
 * 엔티티별 AttributeContainer를 관리하고 제공하는 서비스입니다.
 */
public class StatManager {

    // 임시 메모리 저장소. 실제 구현에서는 ECS나 컴포넌트에 연결해야 함.
    private final Map<UUID, AttributeContainer> entityStats = new ConcurrentHashMap<>();

    public StatManager() {
    }

    /**
     * 엔티티의 스탯 컨테이너를 가져옵니다.
     * 없으면 새로 생성합니다 (몬스터의 경우).
     */
    public AttributeContainer getStats(LivingEntity entity) {
        return entityStats.computeIfAbsent(entity.getUniqueId(), k -> {
            AttributeContainer container = new AttributeContainer();
            initializeDefaultStats(entity, container);
            return container;
        });
    }

    /**
     * 엔티티 제거 시 메모리 정리
     */
    public void removeStats(LivingEntity entity) {
        entityStats.remove(entity.getUniqueId());
    }

    /**
     * 엔티티의 기본 스탯 초기화 (테스트용)
     */
    private void initializeDefaultStats(LivingEntity entity, AttributeContainer container) {
        if (entity instanceof Player) {
            // 플레이어 기본 스탯
            container.setBase(Stat.MAX_HP, 100.0);
            container.setBase(Stat.WEAPON_DAMAGE, 10.0); // 주먹 데미지
            container.setBase(Stat.CRIT_CHANCE, 0.05); // 5%
            container.setBase(Stat.CRIT_DAMAGE, 0.50); // 50%
            container.setBase(Stat.OVERPOWER_DAMAGE, 0.50); // 50%
        } else {
            // 몬스터 기본 스탯
            container.setBase(Stat.MAX_HP, entity.getMaxHealth());
            container.setBase(Stat.WEAPON_DAMAGE, 5.0);
        }
    }
}
