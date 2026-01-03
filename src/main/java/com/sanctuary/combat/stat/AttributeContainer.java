package com.sanctuary.combat.stat;

/**
 * 각 엔티티(플레이어/몬스터)가 가지는 스탯 컨테이너.
 * Base(기본), Additive(합연산), Multiplicative(곱연산) 레이어를 분리하여 관리합니다.
 * SanctuaryCore의 AttributeComponent를 래핑합니다.
 */
public class AttributeContainer {

    private final com.sanctuary.core.ecs.component.AttributeComponent component;

    public AttributeContainer() {
        this(new com.sanctuary.core.ecs.component.AttributeComponent());
    }

    public AttributeContainer(com.sanctuary.core.ecs.component.AttributeComponent component) {
        this.component = component;
    }

    /**
     * 특정 스탯의 최종 값을 조회합니다.
     */
    public double getValue(Stat stat) {
        return component.getValue(stat.name());
    }

    public void setBase(Stat stat, double value) {
        component.setBase(stat.name(), value);
    }

    public void addBase(Stat stat, double value) {
        component.addModifier(stat.name(), value, com.sanctuary.core.ecs.component.ModifierType.BASE);
    }

    public void addAdditive(Stat stat, double value) {
        component.addModifier(stat.name(), value, com.sanctuary.core.ecs.component.ModifierType.ADDITIVE);
    }

    public void addMultiplicative(Stat stat, double value) {
        component.addModifier(stat.name(), value, com.sanctuary.core.ecs.component.ModifierType.MULTIPLICATIVE);
    }

    /**
     * 모든 스탯 초기화
     */
    public void reset() {
        component.clear();
    }

    public com.sanctuary.core.ecs.component.AttributeComponent getComponent() {
        return component;
    }
}
