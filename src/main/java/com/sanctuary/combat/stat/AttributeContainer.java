package com.sanctuary.combat.stat;

import java.util.EnumMap;
import java.util.Map;

/**
 * 각 엔티티(플레이어/몬스터)가 가지는 스탯 컨테이너.
 * Base(기본), Additive(합연산), Multiplicative(곱연산) 레이어를 분리하여 관리합니다.
 */
public class AttributeContainer {

    // 각 스탯별 최종 값 캐싱 (자주 조회되므로)
    private final Map<Stat, Double> cachedValues = new EnumMap<>(Stat.class);

    // 내부 계산용 레이어 (필요시 구현 확장 가능, 현재는 단순화)
    private final Map<Stat, Double> baseValues = new EnumMap<>(Stat.class);
    private final Map<Stat, Double> additiveValues = new EnumMap<>(Stat.class);
    private final Map<Stat, Double> multiplicativeValues = new EnumMap<>(Stat.class);

    public AttributeContainer() {
        // 초기화
        for (Stat stat : Stat.values()) {
            baseValues.put(stat, 0.0);
            additiveValues.put(stat, 0.0);
            multiplicativeValues.put(stat, 1.0); // 곱연산 기본값은 1.0
            cachedValues.put(stat, 0.0);
        }
    }

    /**
     * 특정 스탯의 최종 값을 조회합니다.
     */
    public double getValue(Stat stat) {
        return cachedValues.getOrDefault(stat, 0.0);
    }

    public void setBase(Stat stat, double value) {
        baseValues.put(stat, value);
        recalculate(stat);
    }

    public void addBase(Stat stat, double value) {
        baseValues.put(stat, baseValues.getOrDefault(stat, 0.0) + value);
        recalculate(stat);
    }

    public void addAdditive(Stat stat, double value) {
        additiveValues.put(stat, additiveValues.getOrDefault(stat, 0.0) + value);
        recalculate(stat);
    }

    public void addMultiplicative(Stat stat, double value) {
        // 곱연산은 기존 값에 곱해짐. 예: 10% 증가 -> 1.1을 곱함
        multiplicativeValues.put(stat, multiplicativeValues.getOrDefault(stat, 1.0) * value);
        recalculate(stat);
    }

    /**
     * 스탯 재계산 로직
     * Final = Base * (1 + Additive) * Multiplicative
     */
    private void recalculate(Stat stat) {
        double base = baseValues.getOrDefault(stat, 0.0);
        double add = additiveValues.getOrDefault(stat, 0.0);
        double multi = multiplicativeValues.getOrDefault(stat, 1.0);

        // 예외 처리: 크리티컬 확률 같은건 Base가 퍼센트일 수 있음.
        // 여기서는 일반적인 공식을 따름.
        double finalValue = base * (1.0 + add) * multi;

        // 특수 케이스: 합연산만 적용되는 스탯들 (예: CRIT_DAMAGE)
        // 일부 스탯은 base가 0이고 additive만 있을 수 있음.
        // 설계 보고서에 따르면: MainStatBonus = MainStat * 0.1% (별도)

        cachedValues.put(stat, finalValue);
    }

    /**
     * 모든 스탯 초기화 (장비 교체 시 등)
     */
    public void reset() {
        for (Stat stat : Stat.values()) {
            baseValues.put(stat, 0.0);
            additiveValues.put(stat, 0.0);
            multiplicativeValues.put(stat, 1.0);
            cachedValues.put(stat, 0.0);
        }
    }
}
