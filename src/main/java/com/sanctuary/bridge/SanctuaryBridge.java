package com.sanctuary.bridge;

import com.sanctuary.DiabloPlugin;

/**
 * SanctuaryBridge (신경망)
 * 역할: 네트워크 패킷 처리(Protobuf), 클라이언트 UI 통신
 */
public class SanctuaryBridge {

    private final DiabloPlugin plugin;

    public SanctuaryBridge(DiabloPlugin plugin) {
        this.plugin = plugin;
    }

    public void initialize() {
        // TODO: PacketManager (Netty 채널 훅) 초기화
        // TODO: InventorySyncService (인벤토리 동기화) 초기화
    }

    public void shutdown() {

    }
}
