package com.sanctuary.combat.event;

/**
 * 기본 전투 이벤트 구현
 * 모든 구체적인 전투 이벤트의 기반 클래스입니다.
 */
public abstract class AbstractCombatEvent implements CombatEvent {

    protected final CombatEventType type;
    protected final CombatContext context;
    protected boolean cancelled = false;

    public AbstractCombatEvent(CombatEventType type, CombatContext context) {
        this.type = type;
        this.context = context;
    }

    @Override
    public CombatEventType getType() {
        return type;
    }

    @Override
    public CombatContext getContext() {
        return context;
    }

    @Override
    public boolean isCancelled() {
        return cancelled;
    }

    @Override
    public void setCancelled(boolean cancelled) {
        this.cancelled = cancelled;
    }
}
