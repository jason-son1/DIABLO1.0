package com.sanctuary.combat.event;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * CombatEventBus 단위 테스트
 */
public class CombatEventBusTest {

    private CombatEventBus eventBus;
    private int listenerCallCount;
    private CombatEvent lastReceivedEvent;

    @BeforeEach
    void setUp() {
        // ScriptEngine 없이 테스트 (Lua 훅 비활성화)
        eventBus = new CombatEventBus(
                java.util.logging.Logger.getLogger("CombatEventBusTest"),
                null);
        listenerCallCount = 0;
        lastReceivedEvent = null;
    }

    @Test
    void testEventFiring() {
        // 리스너 등록
        eventBus.register(CombatEventType.DAMAGE_DEALT, event -> {
            listenerCallCount++;
            lastReceivedEvent = event;
        });

        // 이벤트 발생
        CombatContext ctx = CombatContext.builder().skillId("fireball").build();
        DamageDealtEvent event = new DamageDealtEvent(ctx, 100.0);

        eventBus.fire(event);

        // 검증
        assertEquals(1, listenerCallCount);
        assertNotNull(lastReceivedEvent);
        assertSame(event, lastReceivedEvent);
    }

    @Test
    void testMultipleListeners() {
        int[] callCounts = new int[3];

        eventBus.register(CombatEventType.DAMAGE_DEALT, event -> callCounts[0]++);
        eventBus.register(CombatEventType.DAMAGE_DEALT, event -> callCounts[1]++);
        eventBus.register(CombatEventType.DAMAGE_DEALT, event -> callCounts[2]++);

        CombatContext ctx = CombatContext.builder().build();
        eventBus.fire(new DamageDealtEvent(ctx, 50.0));

        assertEquals(1, callCounts[0]);
        assertEquals(1, callCounts[1]);
        assertEquals(1, callCounts[2]);
    }

    @Test
    void testEventCancellation() {
        eventBus.register(CombatEventType.DAMAGE_DEALT, event -> {
            event.setCancelled(true);
        });

        CombatContext ctx = CombatContext.builder().build();
        DamageDealtEvent event = new DamageDealtEvent(ctx, 100.0);

        boolean cancelled = eventBus.fire(event);

        assertTrue(cancelled);
        assertTrue(event.isCancelled());
    }

    @Test
    void testGlobalListener() {
        eventBus.registerGlobal(event -> {
            listenerCallCount++;
        });

        CombatContext ctx = CombatContext.builder().build();

        eventBus.fire(new DamageDealtEvent(ctx, 100.0));
        eventBus.fire(new DamageDealtEvent(ctx, 50.0));

        assertEquals(2, listenerCallCount);
    }

    @Test
    void testDifferentEventTypes() {
        int[] typeCounts = new int[2];

        eventBus.register(CombatEventType.DAMAGE_DEALT, event -> typeCounts[0]++);
        eventBus.register(CombatEventType.CRITICAL_HIT, event -> typeCounts[1]++);

        CombatContext ctx = CombatContext.builder().build();
        eventBus.fire(new DamageDealtEvent(ctx, 100.0));

        assertEquals(1, typeCounts[0]);
        assertEquals(0, typeCounts[1]); // CRITICAL_HIT은 발생하지 않음
    }

    @Test
    void testDamageDealtEventProperties() {
        CombatContext ctx = CombatContext.builder()
                .skillId("fireball")
                .skillCoefficient(1.5)
                .addTags("FIRE", "RANGED")
                .build();

        DamageDealtEvent event = new DamageDealtEvent(ctx, 100.0)
                .finalDamage(150.0)
                .damageType("FIRE")
                .critical(true)
                .overpower(false)
                .vulnerable(true);

        assertEquals(100.0, event.getBaseDamage());
        assertEquals(150.0, event.getFinalDamage());
        assertEquals("FIRE", event.getDamageType());
        assertTrue(event.isCritical());
        assertFalse(event.isOverpower());
        assertTrue(event.isVulnerable());
    }

    @Test
    void testCombatContextBuilder() {
        CombatContext ctx = CombatContext.builder()
                .skillId("whirlwind")
                .skillCoefficient(0.8)
                .distance(3.5)
                .addTag("PHYSICAL")
                .addTags("MELEE", "AOE")
                .put("customKey", 42)
                .build();

        assertEquals("whirlwind", ctx.getSkillId());
        assertEquals(0.8, ctx.getSkillCoefficient());
        assertEquals(3.5, ctx.getDistance());
        assertTrue(ctx.hasTag("PHYSICAL"));
        assertTrue(ctx.hasTag("MELEE"));
        assertTrue(ctx.hasTag("AOE"));
        assertTrue(ctx.hasAnyTag("MELEE", "RANGED"));
        assertEquals(42, (int) ctx.get("customKey"));
    }

    @Test
    void testEventStatistics() {
        CombatContext ctx = CombatContext.builder().build();

        eventBus.fire(new DamageDealtEvent(ctx, 100.0));
        eventBus.fire(new DamageDealtEvent(ctx, 50.0));
        eventBus.fire(new DamageDealtEvent(ctx, 75.0));

        var counts = eventBus.getEventCounts();
        assertEquals(3L, counts.get(CombatEventType.DAMAGE_DEALT));
    }
}
