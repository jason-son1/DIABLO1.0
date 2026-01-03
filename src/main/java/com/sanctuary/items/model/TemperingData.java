package com.sanctuary.items.model;

/**
 * 담금질(Tempering) 상태를 저장하는 클래스입니다.
 */
public class TemperingData {

    private int durability; // 남은 내구도 (기본 5)
    private int maxDurability; // 최대 내구도
    private AffixInstance slot1; // 담금질 슬롯 1
    private AffixInstance slot2; // 담금질 슬롯 2
    private boolean isBricked; // 벽돌 상태 (더 이상 담금질 불가)

    public TemperingData() {
        this.durability = 5;
        this.maxDurability = 5;
        this.isBricked = false;
    }

    /**
     * 담금질 시도가 가능한지 확인합니다.
     */
    public boolean canTemper() {
        return !isBricked && durability > 0;
    }

    /**
     * 내구도를 1 차감합니다.
     * 0이 되면 벽돌 상태가 됩니다.
     */
    public void consumeDurability() {
        durability--;
        if (durability <= 0) {
            isBricked = true;
        }
    }

    // ===== Getters & Setters =====

    public int getDurability() {
        return durability;
    }

    public void setDurability(int durability) {
        this.durability = durability;
    }

    public int getMaxDurability() {
        return maxDurability;
    }

    public void setMaxDurability(int maxDurability) {
        this.maxDurability = maxDurability;
    }

    public AffixInstance getSlot1() {
        return slot1;
    }

    public void setSlot1(AffixInstance slot1) {
        this.slot1 = slot1;
        if (slot1 != null)
            slot1.setTemperingRoll(1);
    }

    public AffixInstance getSlot2() {
        return slot2;
    }

    public void setSlot2(AffixInstance slot2) {
        this.slot2 = slot2;
        if (slot2 != null)
            slot2.setTemperingRoll(2);
    }

    public boolean isBricked() {
        return isBricked;
    }

    public void setBricked(boolean bricked) {
        isBricked = bricked;
    }
}
