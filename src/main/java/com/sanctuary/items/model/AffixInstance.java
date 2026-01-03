package com.sanctuary.items.model;

/**
 * 개별 어픽스 인스턴스를 나타냅니다.
 * 아이템에 부착된 실제 어픽스의 롤링된 값을 저장합니다.
 */
public class AffixInstance {

    private String affixId; // 어픽스 정의 ID (예: "CRIT_CHANCE_01")
    private String statKey; // 스탯 키 (예: "CRIT_CHANCE")
    private double value; // 롤링된 수치
    private double minValue; // 최소값 (참조용)
    private double maxValue; // 최대값 (참조용)
    private boolean isGreater; // Greater Affix (GA) 여부
    private int temperingRoll; // 담금질로 부여된 경우 슬롯 번호 (0=기본, 1-2=담금질)

    public AffixInstance() {
    }

    public AffixInstance(String affixId, String statKey, double value) {
        this.affixId = affixId;
        this.statKey = statKey;
        this.value = value;
        this.isGreater = false;
        this.temperingRoll = 0;
    }

    /**
     * Greater Affix 인스턴스를 생성합니다.
     * 수치가 1.5배로 고정됩니다.
     */
    public static AffixInstance createGreater(String affixId, String statKey, double maxValue) {
        AffixInstance instance = new AffixInstance(affixId, statKey, maxValue * 1.5);
        instance.isGreater = true;
        instance.maxValue = maxValue;
        return instance;
    }

    // ===== Getters & Setters =====

    public String getAffixId() {
        return affixId;
    }

    public void setAffixId(String affixId) {
        this.affixId = affixId;
    }

    public String getStatKey() {
        return statKey;
    }

    public void setStatKey(String statKey) {
        this.statKey = statKey;
    }

    public double getValue() {
        return value;
    }

    public void setValue(double value) {
        this.value = value;
    }

    public double getMinValue() {
        return minValue;
    }

    public void setMinValue(double minValue) {
        this.minValue = minValue;
    }

    public double getMaxValue() {
        return maxValue;
    }

    public void setMaxValue(double maxValue) {
        this.maxValue = maxValue;
    }

    public boolean isGreater() {
        return isGreater;
    }

    public void setGreater(boolean greater) {
        isGreater = greater;
    }

    public int getTemperingRoll() {
        return temperingRoll;
    }

    public void setTemperingRoll(int temperingRoll) {
        this.temperingRoll = temperingRoll;
    }

    /**
     * 롤링 품질을 퍼센트로 반환합니다.
     * (현재값 - 최소값) / (최대값 - 최소값) * 100
     */
    public double getRollQuality() {
        if (maxValue == minValue)
            return 100.0;
        return ((value - minValue) / (maxValue - minValue)) * 100.0;
    }

    @Override
    public String toString() {
        String gaTag = isGreater ? " [GA]" : "";
        return String.format("%s: %.2f%s", statKey, value, gaTag);
    }
}
