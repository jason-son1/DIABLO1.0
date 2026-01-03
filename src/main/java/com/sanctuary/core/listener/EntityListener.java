package com.sanctuary.core.listener;

import com.sanctuary.core.SanctuaryCore;
import com.sanctuary.core.ecs.SanctuaryEntity;
import com.sanctuary.core.ecs.component.AttributeComponent;
import com.sanctuary.core.ecs.component.IdentityComponent;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.entity.EntitySpawnEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

/**
 * 엔티티의 생명주기와 ECS 컴포넌트 연결을 관리하는 리스너입니다.
 */
public class EntityListener implements Listener {

    private final SanctuaryCore core;

    public EntityListener(SanctuaryCore core) {
        this.core = core;
    }

    /**
     * 플레이어 접속 시 SanctuaryEntity를 구성하고 데이터를 로드합니다.
     */
    @EventHandler(priority = EventPriority.LOWEST)
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        SanctuaryEntity sEntity = core.getEntityManager().getOrCreate(player);

        // 기본 컴포넌트 부착
        if (!sEntity.hasComponent(IdentityComponent.class)) {
            IdentityComponent identity = new IdentityComponent("player", 1);
            identity.setDisplayName(player.getName());
            identity.setCategory("HUMAN");
            sEntity.attach(identity);
        }

        if (!sEntity.hasComponent(AttributeComponent.class)) {
            sEntity.attach(new AttributeComponent());
        }

        // TODO: 여기서 PDC로부터 기존 플레이어 데이터를 로드하여 컴포넌트에 주입하는 로직 추가 필요
    }

    /**
     * 플레이어 퇴장 시 메모리에서 제거합니다.
     */
    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        core.getEntityManager().remove(event.getPlayer().getUniqueId());
    }

    /**
     * 몬스터 스폰 시 자동으로 SanctuaryEntity를 구성합니다.
     */
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onEntitySpawn(EntitySpawnEvent event) {
        Entity entity = event.getEntity();

        // LivingEntity(몹, 동물 등)만 관리 대상으로 삼음
        if (!(entity instanceof LivingEntity) || entity instanceof Player) {
            return;
        }

        SanctuaryEntity sEntity = core.getEntityManager().getOrCreate(entity);

        // 몬스터 타입에 기반한 Identity 설정
        String typeId = entity.getType().name().toLowerCase();
        IdentityComponent identity = new IdentityComponent(typeId, 1);
        identity.setDisplayName(entity.getName());
        sEntity.attach(identity);

        // 기본 속성 컴포넌트 부착
        sEntity.attach(new AttributeComponent());

        // TODO: DataRepository에서 해당 몬스터 타입의 기본 스탯을 조회하여 부착하는 로직 추가 필요
    }

    /**
     * 엔티티 사망 시 정리 작업을 수행합니다.
     */
    @EventHandler
    public void onEntityDeath(EntityDeathEvent event) {
        // 즉시 제거하지 않고 EntityManager의 cleanup 스케줄러가 처리하게 하거나
        // 여기서 명시적으로 제거할 수 있음
        core.getEntityManager().remove(event.getEntity().getUniqueId());
    }
}
