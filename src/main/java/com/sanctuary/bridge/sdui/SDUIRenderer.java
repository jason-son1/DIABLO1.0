package com.sanctuary.bridge.sdui;

import com.sanctuary.bridge.packet.PacketManager;
import com.sanctuary.bridge.packet.PacketType;
import com.sanctuary.bridge.packet.SanctuaryPacket;
import com.sanctuary.combat.stat.StatManager;
import com.sanctuary.combat.status.StatusEffect;
import com.sanctuary.combat.status.StatusEffectManager;
import com.sanctuary.core.ecs.EntityManager;
import com.sanctuary.core.ecs.SanctuaryEntity;
import com.sanctuary.core.ecs.component.StateComponent;
import org.bukkit.entity.Player;
import java.util.*;
import java.util.logging.Logger;

/**
 * Server-Driven UI를 렌더링하고 클라이언트에 전송합니다.
 * 플레이어의 HUD 상태를 매 틱마다 동기화합니다.
 */
public class SDUIRenderer {

    private final PacketManager packetManager;
    private final StatManager statManager;
    private final StatusEffectManager statusEffectManager;
    private final EntityManager entityManager;
    private final Logger logger;

    public SDUIRenderer(PacketManager packetManager, StatManager statManager,
            StatusEffectManager statusEffectManager, EntityManager entityManager, Logger logger) {
        this.packetManager = packetManager;
        this.statManager = statManager;
        this.statusEffectManager = statusEffectManager;
        this.entityManager = entityManager;
        this.logger = logger;
    }

    /**
     * 플레이어의 전체 HUD를 렌더링하고 전송합니다.
     */
    public void renderPlayerHUD(Player player) {
        if (!packetManager.hasModClient(player)) {
            return;
        }

        List<UIComponent> hudComponents = new ArrayList<>();

        // 1. 체력 바
        hudComponents.add(buildHealthBar(player));

        // 2. 리소스 바 (마나/분노 등)
        hudComponents.add(buildResourceBar(player));

        // 3. 상태 효과 아이콘들
        hudComponents.addAll(buildStatusIcons(player));

        // 패킷 생성 및 전송
        SanctuaryPacket packet = new SanctuaryPacket(PacketType.S2C_HUD_RENDER);

        List<Map<String, Object>> componentMaps = new ArrayList<>();
        for (UIComponent comp : hudComponents) {
            componentMaps.add(comp.toMap());
        }
        packet.put("components", componentMaps);
        packet.put("timestamp", System.currentTimeMillis());

        packetManager.sendToModClient(player, packet);
    }

    /**
     * 체력 바 컴포넌트를 빌드합니다.
     */
    private UIComponent buildHealthBar(Player player) {
        double currentHp = player.getHealth();
        double maxHp = player.getAttribute(org.bukkit.attribute.Attribute.GENERIC_MAX_HEALTH).getValue();
        double barrier = 0.0;

        // StateComponent에서 보호막 조회
        SanctuaryEntity entity = entityManager.get(player);
        if (entity != null) {
            StateComponent state = entity.getComponent(StateComponent.class);
            if (state != null) {
                barrier = state.getBarrierAmount();
            }
        }

        return UIComponent.healthBar("player_health", currentHp, maxHp, barrier)
                .position(10, 10)
                .size(200, 20)
                .color("#FF4444");
    }

    /**
     * 리소스 바 컴포넌트를 빌드합니다.
     */
    private UIComponent buildResourceBar(Player player) {
        // 기본 리소스: 마나 (스탯에서 조회)
        var stats = statManager.getStats(player);
        double maxResource = stats.getValue(com.sanctuary.combat.stat.Stat.MAX_RESOURCE);
        double resourceRegen = stats.getValue(com.sanctuary.combat.stat.Stat.RESOURCE_REGEN);
        // 현재 리소스는 별도 상태 추적 필요 (일단 maxResource 사용)
        double currentResource = maxResource;
        String resourceType = "MANA"; // 직업별로 변경 가능

        return UIComponent.resourceBar("player_resource", currentResource, maxResource, resourceType)
                .position(10, 35)
                .size(200, 15)
                .color("#4488FF");
    }

    /**
     * 상태 효과 아이콘들을 빌드합니다.
     */
    private List<UIComponent> buildStatusIcons(Player player) {
        List<UIComponent> icons = new ArrayList<>();
        Collection<StatusEffect> effects = statusEffectManager.getEffects(player);

        int index = 0;
        for (StatusEffect effect : effects) {
            UIComponent icon = UIComponent.statusIcon(
                    "status_" + effect.getId().toLowerCase(),
                    effect.getId(),
                    effect.getDurationSeconds(),
                    effect.getStacks())
                    .position(220 + (index * 25), 10)
                    .size(20, 20);

            // 버프/디버프 색상
            if (effect.isBuff()) {
                icon.color("#44FF44");
            } else if (effect.isDebuff()) {
                icon.color("#FF4444");
            } else {
                icon.color("#FFFF44"); // CC
            }

            icons.add(icon);
            index++;
        }

        return icons;
    }

    /**
     * 특정 컴포넌트만 업데이트합니다 (전체 렌더링보다 효율적).
     */
    public void updateComponent(Player player, UIComponent component) {
        if (!packetManager.hasModClient(player)) {
            return;
        }

        SanctuaryPacket packet = new SanctuaryPacket(PacketType.S2C_UI_UPDATE);
        packet.put("component", component.toMap());
        packet.put("updateType", "PARTIAL");

        packetManager.sendToModClient(player, packet);
    }

    /**
     * 데미지 인디케이터를 전송합니다.
     */
    public void showDamageNumber(Player player, double damage, boolean isCrit, boolean isOverpower,
            double x, double y, double z) {
        if (!packetManager.hasModClient(player)) {
            return;
        }

        SanctuaryPacket packet = new SanctuaryPacket(PacketType.S2C_UI_UPDATE);
        packet.put("type", "DAMAGE_NUMBER");
        packet.put("damage", damage);
        packet.put("isCrit", isCrit);
        packet.put("isOverpower", isOverpower);
        packet.put("x", x);
        packet.put("y", y);
        packet.put("z", z);

        packetManager.sendToModClient(player, packet);
    }

    /**
     * 툴팁을 표시합니다.
     */
    public void showTooltip(Player player, String title, List<String> lines, int x, int y) {
        if (!packetManager.hasModClient(player)) {
            return;
        }

        UIComponent tooltip = UIComponent.tooltip("dynamic_tooltip", title, lines)
                .position(x, y);

        SanctuaryPacket packet = new SanctuaryPacket(PacketType.S2C_UI_UPDATE);
        packet.put("component", tooltip.toMap());
        packet.put("updateType", "SHOW");

        packetManager.sendToModClient(player, packet);
    }
}
