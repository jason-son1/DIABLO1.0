package com.sanctuary.core.ecs;

import com.sanctuary.core.ecs.component.AttributeComponent;
import com.sanctuary.core.ecs.component.ModifierType;
import com.sanctuary.core.ecs.component.StatValue;
import com.sanctuary.core.ecs.component.TagComponent;
import com.sanctuary.core.ecs.component.IdentityComponent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * ECS 시스템 단위 테스트
 */
public class EcsTest {

    private ComponentContainer container;

    @BeforeEach
    void setUp() {
        container = new ComponentContainer();
    }

    // ===== StatValue 테스트 =====

    @Test
    void testStatValueCalculation() {
        StatValue stat = new StatValue(100.0); // base = 100
        stat.addAdditive(0.2); // +20%
        stat.multiplyMultiplicative(1.15); // x1.15

        // 100 * (1 + 0.2) * 1.15 = 138
        assertEquals(138.0, stat.getFinalValue(), 0.01);
    }

    @Test
    void testStatValueWithMultipleModifiers() {
        StatValue stat = new StatValue(50.0);
        stat.addAdditive(0.1); // +10%
        stat.addAdditive(0.2); // +20% (총 +30%)
        stat.multiplyMultiplicative(1.1); // x1.1
        stat.multiplyMultiplicative(1.2); // x1.2 (총 x1.32)

        // 50 * (1 + 0.3) * 1.32 = 85.8
        assertEquals(85.8, stat.getFinalValue(), 0.01);
    }

    @Test
    void testStatValueReset() {
        StatValue stat = new StatValue(100.0);
        stat.addAdditive(0.5);
        stat.multiplyMultiplicative(2.0);

        stat.resetModifiers();

        // 수정자 리셋 후 기본값만 남음
        assertEquals(100.0, stat.getFinalValue(), 0.01);
        assertEquals(0.0, stat.getAdditive(), 0.01);
        assertEquals(1.0, stat.getMultiplicative(), 0.01);
    }

    // ===== ComponentContainer 테스트 =====

    @Test
    void testComponentAttachAndGet() {
        TagComponent tags = new TagComponent();
        tags.add("ELITE");

        container.attach(tags);

        assertTrue(container.has(TagComponent.class));
        assertEquals(tags, container.get(TagComponent.class));
    }

    @Test
    void testComponentOptional() {
        assertFalse(container.getOptional(TagComponent.class).isPresent());

        container.attach(new TagComponent());

        assertTrue(container.getOptional(TagComponent.class).isPresent());
    }

    @Test
    void testComponentDetach() {
        container.attach(new TagComponent());
        assertTrue(container.has(TagComponent.class));

        container.detach(TagComponent.class);
        assertFalse(container.has(TagComponent.class));
    }

    // ===== AttributeComponent 테스트 =====

    @Test
    void testAttributeComponentModifiers() {
        AttributeComponent attrs = new AttributeComponent();

        attrs.setBase("STRENGTH", 100.0);
        attrs.addModifier("STRENGTH", 0.2, ModifierType.ADDITIVE);
        attrs.addModifier("STRENGTH", 1.15, ModifierType.MULTIPLICATIVE);

        // 100 * (1 + 0.2) * 1.15 = 138
        assertEquals(138.0, attrs.getValue("STRENGTH"), 0.01);
    }

    @Test
    void testAttributeComponentNonExistent() {
        AttributeComponent attrs = new AttributeComponent();

        // 존재하지 않는 스탯은 0 반환
        assertEquals(0.0, attrs.getValue("NON_EXISTENT"), 0.01);
    }

    // ===== TagComponent 테스트 =====

    @Test
    void testTagComponentOperations() {
        TagComponent tags = new TagComponent();

        tags.add("ELITE").add("UNDEAD");

        assertTrue(tags.has("ELITE"));
        assertTrue(tags.has("undead")); // 대소문자 무관
        assertFalse(tags.has("BOSS"));

        tags.remove("ELITE");
        assertFalse(tags.has("ELITE"));
    }

    @Test
    void testTagComponentHasAny() {
        TagComponent tags = new TagComponent();
        tags.addAll("UNDEAD", "DEMON");

        assertTrue(tags.hasAny("HUMAN", "UNDEAD"));
        assertFalse(tags.hasAny("BEAST", "ANGEL"));
    }

    @Test
    void testTagComponentHasAll() {
        TagComponent tags = new TagComponent();
        tags.addAll("ELITE", "UNDEAD", "BOSS");

        assertTrue(tags.hasAll("ELITE", "BOSS"));
        assertFalse(tags.hasAll("ELITE", "HUMAN"));
    }

    // ===== IdentityComponent 테스트 =====

    @Test
    void testIdentityComponent() {
        IdentityComponent identity = new IdentityComponent("skeleton_warrior", 50);
        identity.setCategory("ELITE").setFamily("UNDEAD");

        assertEquals("skeleton_warrior", identity.getTemplateId());
        assertEquals(50, identity.getLevel());
        assertTrue(identity.isElite());
        assertFalse(identity.isBoss());
    }
}
