package com.sanctuary.bridge.sync;

import com.sanctuary.bridge.packet.PacketManager;
import com.sanctuary.bridge.packet.SanctuaryPacket;
import com.sanctuary.bridge.sdui.SduiComponent;
import com.sanctuary.combat.stat.AttributeContainer;
import com.sanctuary.combat.stat.Stat;
import com.sanctuary.combat.stat.StatManager;
import com.sanctuary.combat.status.StatusEffect;
import com.sanctuary.combat.status.StatusEffectManager;
import org.bukkit.entity.Player;

import java.util.*;
import java.util.logging.Logger;

/**
 * 플레이어 스탯을 클라이언트와 동기화하는 서비스입니다.
 */
public class StatSyncService {

    private final PacketManager packetManager;
    private final StatManager statManager;
    private final StatusEffectManager statusManager;
    private final Logger logger;

    // 마지막 동기화 시각
    private final Map<UUID, Long> lastSyncTimes = new HashMap<>();
    private static final long SYNC_COOLDOWN_MS = 100; // 100ms 쿨다운

    public StatSyncService(PacketManager packetManager, StatManager statManager,
            StatusEffectManager statusManager, Logger logger) {
        this.packetManager = packetManager;
        this.statManager = statManager;
        this.statusManager = statusManager;
        this.logger = logger;
    }

    /**
     * 플레이어의 스탯을 클라이언트에 동기화합니다.
     */
    public void syncStats(Player player) {
        if (!packetManager.hasModClient(player))
            return;

        // 쿨다운 체크
        long now = System.currentTimeMillis();
        Long lastSync = lastSyncTimes.get(player.getUniqueId());
        if (lastSync != null && now - lastSync < SYNC_COOLDOWN_MS)
            return;
        lastSyncTimes.put(player.getUniqueId(), now);

        AttributeContainer stats = statManager.getStats(player);

        Map<String, Double> statMap = new HashMap<>();
        statMap.put("HP", player.getHealth());
        statMap.put("MAX_HP", player.getMaxHealth());
        statMap.put("WEAPON_DAMAGE", stats.getValue(Stat.WEAPON_DAMAGE));
        statMap.put("CRIT_CHANCE", stats.getValue(Stat.CRIT_CHANCE));
        statMap.put("CRIT_DAMAGE", stats.getValue(Stat.CRIT_DAMAGE));
        statMap.put("ARMOR", stats.getValue(Stat.ARMOR));
        statMap.put("ATTACK_SPEED", stats.getValue(Stat.ATTACK_SPEED));

        SanctuaryPacket packet = SanctuaryPacket.syncStats(statMap);
        packetManager.send(player, packet);
    }

    /**
     * 플레이어의 상태 이상을 클라이언트에 동기화합니다.
     */
    public void syncStatusEffects(Player player) {
        if (!packetManager.hasModClient(player))
            return;

        Collection<StatusEffect> effects = statusManager.getEffects(player);

        List<Map<String, Object>> effectList = new ArrayList<>();
        for (StatusEffect effect : effects) {
            Map<String, Object> effectData = new HashMap<>();
            effectData.put("id", effect.getId());
            effectData.put("name", effect.getDisplayName());
            effectData.put("type", effect.getType().name());
            effectData.put("stacks", effect.getStacks());
            effectData.put("duration", effect.getDurationSeconds());
            effectList.add(effectData);
        }

        SanctuaryPacket packet = new SanctuaryPacket(com.sanctuary.bridge.packet.PacketType.S2C_SYNC_STATUS_EFFECTS)
                .put("effects", effectList);
        packetManager.send(player, packet);
    }

    /**
     * HUD UI를 전송합니다.
     */
    public void sendHudUpdate(Player player) {
        if (!packetManager.hasModClient(player))
            return;

        AttributeContainer stats = statManager.getStats(player);

        // 체력/자원 오브
        SduiComponent healthOrb = SduiComponent.healthOrb("health_orb",
                player.getHealth(), player.getMaxHealth());

        SduiComponent resourceOrb = SduiComponent.resourceOrb("resource_orb",
                stats.getValue(Stat.MAX_RESOURCE) * 0.7, // 예시: 70%
                stats.getValue(Stat.MAX_RESOURCE),
                "MANA");

        Map<String, Object> hudData = new HashMap<>();
        hudData.put("healthOrb", healthOrb.toMap());
        hudData.put("resourceOrb", resourceOrb.toMap());

        SanctuaryPacket packet = new SanctuaryPacket(com.sanctuary.bridge.packet.PacketType.S2C_UI_UPDATE)
                .put("hudType", "MAIN_HUD")
                .put("components", hudData);
        packetManager.send(player, packet);
    }

    /**
     * 보스 바 UI를 전송합니다.
     */
    public void sendBossBar(Player player, String bossName, double hp, double maxHp,
            double stagger, double maxStagger) {
        if (!packetManager.hasModClient(player))
            return;

        SduiComponent bossBar = SduiComponent.bossBar("boss_bar",
                bossName, hp, maxHp, stagger, maxStagger);

        SanctuaryPacket packet = new SanctuaryPacket(com.sanctuary.bridge.packet.PacketType.S2C_BOSS_BAR)
                .put("component", bossBar.toMap());
        packetManager.send(player, packet);
    }

    /**
     * 데미지 인디케이터를 전송합니다.
     */
    public void sendDamageIndicator(Player player, double damage, boolean crit, boolean overpower,
            double x, double y, double z) {
        if (!packetManager.hasModClient(player))
            return;

        SanctuaryPacket packet = SanctuaryPacket.damageIndicator(damage, crit, overpower, x, y, z);
        packetManager.send(player, packet);
    }

    /**
     * 정리
     */
    public void cleanup(Player player) {
        lastSyncTimes.remove(player.getUniqueId());
    }
}
