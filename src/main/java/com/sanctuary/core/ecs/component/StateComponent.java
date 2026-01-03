package com.sanctuary.core.ecs.component;

import com.sanctuary.core.ecs.Component;

/**
 * 엔티티의 현재 상태를 관리하는 컴포넌트입니다.
 * 그로기(Stagger) 게이지, CC 상태, 동작 상태 등을 추적합니다.
 */
public class StateComponent implements Component {

    // ===== 그로기(Stagger) 시스템 - 보스 전용 =====
    private double staggerGauge = 0.0; // 현재 그로기 게이지 (0~100)
    private double staggerMax = 100.0; // 최대 그로기 게이지
    private boolean staggered = false; // 그로기 상태 여부
    private long staggerEndTime = 0; // 그로기 종료 시각 (ms)

    // ===== 현재 동작 상태 =====
    private ActionState actionState = ActionState.IDLE;
    private long actionEndTime = 0; // 현재 동작 종료 시각

    // ===== 보호막(Barrier) =====
    private double barrierAmount = 0.0; // 현재 보호막 량
    private double barrierMax = 0.0; // 최대 보호막 량

    // ===== 보강(Fortify) =====
    private double fortifyAmount = 0.0; // 현재 보강 량

    /**
     * 가능한 동작 상태를 정의합니다.
     */
    public enum ActionState {
        IDLE, // 대기
        CASTING, // 시전 중
        ATTACKING, // 공격 중
        STUNNED, // 기절
        CHANNELING, // 채널링 중
        RECOVERING // 회복 중
    }

    // ===== 그로기 관련 메서드 =====

    /**
     * 그로기 게이지에 피해를 추가합니다.
     * 
     * @param amount 추가할 양
     * @return 그로기 발생 여부
     */
    public boolean addStagger(double amount) {
        if (staggered)
            return false;

        staggerGauge = Math.min(staggerGauge + amount, staggerMax);
        if (staggerGauge >= staggerMax) {
            staggered = true;
            staggerEndTime = System.currentTimeMillis() + 10000; // 10초 그로기
            staggerGauge = 0;
            return true;
        }
        return false;
    }

    /**
     * 그로기 상태를 갱신합니다.
     * 매 틱마다 호출해야 합니다.
     */
    public void updateStagger() {
        if (staggered && System.currentTimeMillis() >= staggerEndTime) {
            staggered = false;
        }
    }

    public double getStaggerGauge() {
        return staggerGauge;
    }

    public double getStaggerMax() {
        return staggerMax;
    }

    public void setStaggerMax(double staggerMax) {
        this.staggerMax = staggerMax;
    }

    public boolean isStaggered() {
        updateStagger();
        return staggered;
    }

    public double getStaggerPercent() {
        return (staggerGauge / staggerMax) * 100.0;
    }

    // ===== 동작 상태 관련 메서드 =====

    public ActionState getActionState() {
        updateActionState();
        return actionState;
    }

    public void setActionState(ActionState state, long durationMs) {
        this.actionState = state;
        this.actionEndTime = System.currentTimeMillis() + durationMs;
    }

    public void setActionState(ActionState state) {
        this.actionState = state;
        this.actionEndTime = 0;
    }

    private void updateActionState() {
        if (actionEndTime > 0 && System.currentTimeMillis() >= actionEndTime) {
            actionState = ActionState.IDLE;
            actionEndTime = 0;
        }
    }

    public boolean isIdle() {
        return getActionState() == ActionState.IDLE;
    }

    public boolean canAct() {
        ActionState state = getActionState();
        return state == ActionState.IDLE || state == ActionState.RECOVERING;
    }

    // ===== 보호막 관련 메서드 =====

    public double getBarrierAmount() {
        return barrierAmount;
    }

    public void setBarrierAmount(double amount) {
        this.barrierAmount = Math.max(0, Math.min(amount, barrierMax));
    }

    public void addBarrier(double amount) {
        setBarrierAmount(barrierAmount + amount);
    }

    /**
     * 보호막에 피해를 적용합니다.
     * 
     * @param damage 피해량
     * @return 보호막이 흡수하지 못한 남은 피해량
     */
    public double damageBarrier(double damage) {
        if (barrierAmount <= 0)
            return damage;

        double absorbed = Math.min(barrierAmount, damage);
        barrierAmount -= absorbed;
        return damage - absorbed;
    }

    public double getBarrierMax() {
        return barrierMax;
    }

    public void setBarrierMax(double barrierMax) {
        this.barrierMax = barrierMax;
    }

    public boolean hasBarrier() {
        return barrierAmount > 0;
    }

    // ===== 보강 관련 메서드 =====

    public double getFortifyAmount() {
        return fortifyAmount;
    }

    public void setFortifyAmount(double amount) {
        this.fortifyAmount = Math.max(0, amount);
    }

    public void addFortify(double amount) {
        setFortifyAmount(fortifyAmount + amount);
    }

    /**
     * 보강이 체력 이상인지 확인합니다.
     * 보강 >= 현재 체력일 때 피해 15% 감소 효과가 적용됩니다.
     * 
     * @param currentHp 현재 체력
     * @return 보강 활성화 여부
     */
    public boolean isFortified(double currentHp) {
        return fortifyAmount >= currentHp;
    }

    @Override
    public String toString() {
        return String.format("StateComponent{action=%s, stagger=%.1f%%, barrier=%.1f, fortify=%.1f}",
                actionState, getStaggerPercent(), barrierAmount, fortifyAmount);
    }
}
