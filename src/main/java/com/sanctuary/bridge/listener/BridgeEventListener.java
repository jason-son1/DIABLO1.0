package com.sanctuary.bridge.listener;

import com.sanctuary.bridge.SanctuaryBridge;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

/**
 * 브릿지 관련 이벤트 리스너입니다.
 */
public class BridgeEventListener implements Listener {

    private final SanctuaryBridge bridge;

    public BridgeEventListener(SanctuaryBridge bridge) {
        this.bridge = bridge;
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        // 플레이어 입장 시 처리
        // 모드 클라이언트 여부는 핸드셰이크 패킷으로 확인됨
        bridge.getPlugin().getLogger().fine("[Bridge] 플레이어 입장: " + event.getPlayer().getName());
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        // 플레이어 퇴장 시 정리
        bridge.getPacketManager().onPlayerQuit(event.getPlayer());
        bridge.getStatSyncService().cleanup(event.getPlayer());
    }
}
