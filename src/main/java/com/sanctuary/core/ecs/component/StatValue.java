package com.sanctuary.core.ecs.component;

/**
 * 단일 스탯의 합연산/곱연산 값을 관리하는 클래스입니다.
 * 디아블로 IV 시즌 6의 데미지 버킷 공식을 구현합니다.
 * 
 * 최종 값 공식: base * (1 + additive) * multiplicative
 * 
 * 예시:
 * - base = 100 (기본 공격력)
 * - additive = 0.35 (+20% + +15% 합연산 보너스)
 * - multiplicative = 1.38 (1.15 * 1.20 곱연산 보너스)
 * - 최종 값 = 100 * (1 + 0.35) * 1.38 = 186.3
 */
public class StatValue {

    private double base;
    private double additive;
    private double multiplicative;

    /**
     * 기본값 0으로 StatValue를 생성합니다.
     */
    public StatValue() {
        this(0.0);
    }

    /**
     * 지정된 기본값으로 StatValue를 생성합니다.
     * 
     * @param base 기본값
     */
    public StatValue(double base) {
        this.base = base;
        this.additive = 0.0;
        this.multiplicative = 1.0;
    }

    /**
     * 최종 스탯 값을 계산합니다.
     * 공식: base * (1 + additive) * multiplicative
     * 
     * @return 최종 계산된 값
     */
    public double getFinalValue() {
        return base * (1.0 + additive) * multiplicative;
    }

    // ===== 기본값 관련 =====

    public double getBase() {
        return base;
    }

    public void setBase(double base) {
        this.base = base;
    }

    public void addBase(double amount) {
        this.base += amount;
    }

    // ===== 합연산 관련 =====

    public double getAdditive() {
        return additive;
    }

    /**
     * 합연산 보너스를 추가합니다.
     * 예: addAdditive(0.2) → +20% 합연산 보너스 추가
     * 
     * @param amount 추가할 퍼센트 (0.2 = 20%)
     */
    public void addAdditive(double amount) {
        this.additive += amount;
    }

    public void setAdditive(double additive) {
        this.additive = additive;
    }

    // ===== 곱연산 관련 =====

    public double getMultiplicative() {
        return multiplicative;
    }

    /**
     * 곱연산 보너스를 적용합니다.
     * 예: multiplyMultiplicative(1.15) → x1.15 곱연산 보너스 적용
     * 
     * @param factor 곱할 배율 (1.15 = 15% 증가)
     */
    public void multiplyMultiplicative(double factor) {
        this.multiplicative *= factor;
    }

    public void setMultiplicative(double multiplicative) {
        this.multiplicative = multiplicative;
    }

    // ===== 유틸리티 =====

    /**
     * 모든 수정자를 초기화합니다.
     * 기본값은 유지하고 합연산/곱연산만 리셋합니다.
     */
    public void resetModifiers() {
        this.additive = 0.0;
        this.multiplicative = 1.0;
    }

    /**
     * 모든 값을 초기화합니다.
     */
    public void reset() {
        this.base = 0.0;
        this.additive = 0.0;
        this.multiplicative = 1.0;
    }

    /**
     * 다른 StatValue의 값을 이 인스턴스에 병합합니다.
     * 
     * @param other 병합할 StatValue
     */
    public void merge(StatValue other) {
        this.base += other.base;
        this.additive += other.additive;
        this.multiplicative *= other.multiplicative;
    }

    /**
     * 현재 상태의 복사본을 생성합니다.
     * 
     * @return StatValue 복사본
     */
    public StatValue copy() {
        StatValue copy = new StatValue(this.base);
        copy.additive = this.additive;
        copy.multiplicative = this.multiplicative;
        return copy;
    }

    @Override
    public String toString() {
        return String.format("StatValue{base=%.2f, add=%.2f%%, mult=%.2fx, final=%.2f}",
                base, additive * 100, multiplicative, getFinalValue());
    }
}
