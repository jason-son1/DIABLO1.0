package com.sanctuary.core;

import com.sanctuary.DiabloPlugin;

/**
 * SanctuaryCore (시스템의 두뇌)
 * 역할: 데이터 로드(ETL), ECS(엔티티 컴포넌트) 관리, Lua 스크립트 엔진
 */
public class SanctuaryCore {

    private final DiabloPlugin plugin;

    public SanctuaryCore(DiabloPlugin plugin) {
        this.plugin = plugin;
    }

    public void initialize() {
        // TODO: DataManager (JSON/Protobuf 로더) 초기화
        // TODO: LuaEngine (스크립트 엔진) 초기화
        // TODO: Registry (스탯, 태그 등) 초기화
    }

    public void shutdown() {
        // 리소스 해제
    }
}
