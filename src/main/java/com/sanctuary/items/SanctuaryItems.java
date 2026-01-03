package com.sanctuary.items;

import com.sanctuary.DiabloPlugin;
import com.sanctuary.core.SanctuaryCore;

/**
 * SanctuaryItems (뼈대)
 * 역할: 아이템 생성(RNG), NBT/PDC 직렬화, 인벤토리 데이터 모델
 */
public class SanctuaryItems {

    private final DiabloPlugin plugin;
    private final SanctuaryCore core;

    public SanctuaryItems(DiabloPlugin plugin, SanctuaryCore core) {
        this.plugin = plugin;
        this.core = core;
    }

    public void initialize() {
        // TODO: ItemFactory (아이템 생성기) 초기화
        // TODO: CraftingManager (담금질/명품화) 초기화
    }

    public void shutdown() {

    }
}
