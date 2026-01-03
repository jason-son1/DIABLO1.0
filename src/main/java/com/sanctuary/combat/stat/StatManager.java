package com.sanctuary.combat.stat;

import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;

/**
 * StatManager
 * 엔티티별 AttributeContainer를 관리하고 제공하는 서비스입니다.
 * SanctuaryCore의 ECS 시스템과 연동합니다.
 */
public class StatManager {

    private final com.sanctuary.core.SanctuaryCore core;

    public StatManager(com.sanctuary.core.SanctuaryCore core) {
        this.core = core;
    }

    public com.sanctuary.core.SanctuaryCore getCore() {
        return core;
    }

    /**
     * 엔티티의 스탯 컨테이너를 가져옵니다.
     * SanctuaryCore의 ECS 시스템과 연동합니다.
     */
    public AttributeContainer getStats(LivingEntity entity) {
        com.sanctuary.core.ecs.SanctuaryEntity sEntity = core.getEntityManager().getOrCreate(entity);

        com.sanctuary.core.ecs.component.AttributeComponent attrComp = sEntity
                .getComponent(com.sanctuary.core.ecs.component.AttributeComponent.class);
        if (attrComp == null) {
            attrComp = new com.sanctuary.core.ecs.component.AttributeComponent();
            sEntity.attach(attrComp);
            initializeDefaultStats(entity, new AttributeContainer(attrComp));
        }

        return new AttributeContainer(attrComp);
    }

    /**
     * 엔티티 제거 시 ECS에서도 정리 (필요시)
     */
    public void removeStats(LivingEntity entity) {
        core.getEntityManager().remove(entity.getUniqueId());
    }

    /**
     * 엔티티의 기본 스탯 초기화
     */
    private void initializeDefaultStats(LivingEntity entity, AttributeContainer container) {
        if (entity instanceof Player) {
            // 플레이어 기본 스탯
            container.setBase(Stat.MAX_HP, 100.0);
            container.setBase(Stat.WEAPON_DAMAGE, 10.0);
            container.setBase(Stat.ATTACK_SPEED, 0.0);
            container.setBase(Stat.CRIT_CHANCE, 0.05);
            container.setBase(Stat.CRIT_DAMAGE, 0.50);
            container.setBase(Stat.OVERPOWER_DAMAGE, 0.50);
        } else {
            // 몬스터 기본 스탯
            org.bukkit.attribute.AttributeInstance attr = entity
                    .getAttribute(org.bukkit.attribute.Attribute.GENERIC_MAX_HEALTH);
            double maxHp = attr != null ? attr.getBaseValue() : 20.0;
            container.setBase(Stat.MAX_HP, maxHp);
            container.setBase(Stat.WEAPON_DAMAGE, 5.0);
        }
    }
}
