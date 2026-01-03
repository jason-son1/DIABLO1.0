package com.sanctuary.items.model;

/**
 * 아이템 희귀도를 정의합니다.
 * 디아블로 IV의 희귀도 시스템을 따릅니다.
 */
public enum ItemRarity {

    /**
     * 일반 (Common) - 흰색
     */
    COMMON("일반", "§f", 0),

    /**
     * 마법 (Magic) - 파란색
     */
    MAGIC("마법", "§9", 1),

    /**
     * 희귀 (Rare) - 노란색
     */
    RARE("희귀", "§e", 2),

    /**
     * 전설 (Legendary) - 주황색
     */
    LEGENDARY("전설", "§6", 3),

    /**
     * 고유 (Unique) - 금색/갈색
     */
    UNIQUE("고유", "§4", 4),

    /**
     * 신화 (Mythic) - 보라색
     * 시즌 7 업버 유니크 급
     */
    MYTHIC("신화", "§5", 5);

    private final String displayName;
    private final String colorCode;
    private final int tier;

    ItemRarity(String displayName, String colorCode, int tier) {
        this.displayName = displayName;
        this.colorCode = colorCode;
        this.tier = tier;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getColorCode() {
        return colorCode;
    }

    public int getTier() {
        return tier;
    }

    /**
     * 색상 코드가 적용된 표시 이름을 반환합니다.
     */
    public String getColoredName() {
        return colorCode + displayName;
    }

    /**
     * 이 희귀도의 기본 어픽스 개수를 반환합니다.
     */
    public int getDefaultAffixCount() {
        return switch (this) {
            case COMMON -> 0;
            case MAGIC -> 1;
            case RARE -> 3;
            case LEGENDARY, UNIQUE -> 4;
            case MYTHIC -> 5;
        };
    }
}
