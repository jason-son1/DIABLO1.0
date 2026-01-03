package com.sanctuary.bridge.packet;

import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.messaging.PluginMessageListener;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;
import java.util.logging.Logger;

/**
 * 플러그인 채널을 통한 패킷 송수신을 관리합니다.
 */
public class PacketManager implements PluginMessageListener {

    public static final String CHANNEL = "sanctuary:main";

    private final Plugin plugin;
    private final Logger logger;

    // 패킷 핸들러 맵 (PacketType -> Handler)
    private final Map<PacketType, Consumer<PacketContext>> handlers = new EnumMap<>(PacketType.class);

    // 모드 클라이언트 연결된 플레이어
    private final Set<UUID> modClients = ConcurrentHashMap.newKeySet();

    public PacketManager(Plugin plugin, Logger logger) {
        this.plugin = plugin;
        this.logger = logger;
    }

    /**
     * 채널을 등록하고 리스너를 시작합니다.
     */
    public void initialize() {
        plugin.getServer().getMessenger().registerOutgoingPluginChannel(plugin, CHANNEL);
        plugin.getServer().getMessenger().registerIncomingPluginChannel(plugin, CHANNEL, this);
        logger.info("[PacketManager] 플러그인 채널 등록됨: " + CHANNEL);
    }

    /**
     * 채널을 해제합니다.
     */
    public void shutdown() {
        plugin.getServer().getMessenger().unregisterOutgoingPluginChannel(plugin, CHANNEL);
        plugin.getServer().getMessenger().unregisterIncomingPluginChannel(plugin, CHANNEL, this);
        modClients.clear();
    }

    // ===== 패킷 핸들러 등록 =====

    /**
     * 패킷 핸들러를 등록합니다.
     */
    public void registerHandler(PacketType type, Consumer<PacketContext> handler) {
        handlers.put(type, handler);
    }

    // ===== 패킷 전송 =====

    /**
     * 플레이어에게 패킷을 전송합니다.
     */
    public void send(Player player, SanctuaryPacket packet) {
        if (player == null || !player.isOnline())
            return;

        try {
            byte[] data = packet.serialize();
            player.sendPluginMessage(plugin, CHANNEL, data);
            logger.fine("[PacketManager] 전송: " + packet.getType() + " -> " + player.getName());
        } catch (Exception e) {
            logger.warning("[PacketManager] 전송 실패: " + e.getMessage());
        }
    }

    /**
     * 모드 클라이언트에게만 패킷을 전송합니다.
     */
    public void sendToModClient(Player player, SanctuaryPacket packet) {
        if (!hasModClient(player))
            return;
        send(player, packet);
    }

    /**
     * 모든 모드 클라이언트에게 패킷을 브로드캐스트합니다.
     */
    public void broadcast(SanctuaryPacket packet) {
        for (UUID uuid : modClients) {
            Player player = plugin.getServer().getPlayer(uuid);
            if (player != null && player.isOnline()) {
                send(player, packet);
            }
        }
    }

    // ===== 패킷 수신 =====

    @Override
    public void onPluginMessageReceived(String channel, Player player, byte[] message) {
        if (!CHANNEL.equals(channel))
            return;

        try {
            SanctuaryPacket packet = SanctuaryPacket.deserialize(message);
            if (packet == null) {
                logger.warning("[PacketManager] 잘못된 패킷: " + player.getName());
                return;
            }

            logger.fine("[PacketManager] 수신: " + packet.getType() + " <- " + player.getName());

            // 핸드셰이크 처리
            if (packet.getType() == PacketType.C2S_HANDSHAKE) {
                handleHandshake(player, packet);
                return;
            }

            // 핸들러 호출
            Consumer<PacketContext> handler = handlers.get(packet.getType());
            if (handler != null) {
                handler.accept(new PacketContext(player, packet));
            }

        } catch (Exception e) {
            logger.severe("[PacketManager] 처리 오류: " + e.getMessage());
        }
    }

    /**
     * 클라이언트 핸드셰이크를 처리합니다.
     */
    private void handleHandshake(Player player, SanctuaryPacket packet) {
        String clientVersion = packet.getString("version");
        logger.info("[PacketManager] 모드 클라이언트 연결: " + player.getName() + " (v" + clientVersion + ")");

        modClients.add(player.getUniqueId());

        // 환영 패킷 전송
        SanctuaryPacket response = new SanctuaryPacket(PacketType.S2C_UI_UPDATE)
                .put("message", "Sanctuary 클라이언트 연결됨!");
        send(player, response);
    }

    // ===== 유틸리티 =====

    /**
     * 플레이어가 모드 클라이언트인지 확인합니다.
     */
    public boolean hasModClient(Player player) {
        return player != null && modClients.contains(player.getUniqueId());
    }

    /**
     * 플레이어 연결 해제 시 호출합니다.
     */
    public void onPlayerQuit(Player player) {
        modClients.remove(player.getUniqueId());
    }

    /**
     * 모드 클라이언트 수를 반환합니다.
     */
    public int getModClientCount() {
        return modClients.size();
    }

    // ===== 컨텍스트 클래스 =====

    /**
     * 패킷 처리 컨텍스트
     */
    public record PacketContext(Player player, SanctuaryPacket packet) {
        public <T> T getData(String key) {
            return packet.get(key);
        }
    }
}
