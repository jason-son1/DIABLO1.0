package com.sanctuary.combat.status;

/**
 * 개별 상태 이상 효과를 나타내는 클래스입니다.
 * 디아블로 IV의 핵심 상태(취약, 보강, 출혈 등)를 구현합니다.
 */
public class StatusEffect {

    private final String id; // 상태 ID (예: "VULNERABLE", "BLEEDING")
    private final String displayName; // 표시 이름
    private final StatusType type; // 버프/디버프/CC

    private int durationTicks; // 남은 지속시간 (틱, 20틱 = 1초)
    private int maxDuration; // 최대 지속시간
    private int stacks; // 중첩 수
    private int maxStacks; // 최대 중첩 수
    private double value; // 효과 수치 (예: 출혈 데미지, 이속 감소율)

    private long appliedTime; // 적용 시각 (ms)

    /**
     * 기본 StatusEffect를 생성합니다.
     * 
     * @param id            상태 ID
     * @param displayName   표시 이름
     * @param type          상태 타입
     * @param durationTicks 지속시간 (틱)
     */
    public StatusEffect(String id, String displayName, StatusType type, int durationTicks) {
        this.id = id.toUpperCase();
        this.displayName = displayName;
        this.type = type;
        this.durationTicks = durationTicks;
        this.maxDuration = durationTicks;
        this.stacks = 1;
        this.maxStacks = 1;
        this.value = 0.0;
        this.appliedTime = System.currentTimeMillis();
    }

    // ===== 팩토리 메서드 (주요 상태 이상) =====

    /**
     * 취약(Vulnerable) 상태를 생성합니다.
     * 효과: 받는 피해 20% 증가
     */
    public static StatusEffect vulnerable(int durationTicks) {
        StatusEffect effect = new StatusEffect("VULNERABLE", "취약", StatusType.DEBUFF, durationTicks);
        effect.value = 0.2; // 20% 추가 피해
        return effect;
    }

    /**
     * 보강(Fortify) 상태를 생성합니다.
     * 효과: 보강량 충족 시 받는 피해 15% 감소
     */
    public static StatusEffect fortify(int durationTicks, double amount) {
        StatusEffect effect = new StatusEffect("FORTIFY", "보강", StatusType.BUFF, durationTicks);
        effect.value = amount; // 보강 수치
        return effect;
    }

    /**
     * 출혈(Bleeding) 상태를 생성합니다.
     * 효과: 매 초 물리 피해
     */
    public static StatusEffect bleeding(int durationTicks, double damagePerSecond) {
        StatusEffect effect = new StatusEffect("BLEEDING", "출혈", StatusType.DEBUFF, durationTicks);
        effect.value = damagePerSecond;
        effect.maxStacks = 5; // 최대 5중첩
        return effect;
    }

    /**
     * 화상(Burning) 상태를 생성합니다.
     * 효과: 매 초 화염 피해
     */
    public static StatusEffect burning(int durationTicks, double damagePerSecond) {
        StatusEffect effect = new StatusEffect("BURNING", "화상", StatusType.DEBUFF, durationTicks);
        effect.value = damagePerSecond;
        return effect;
    }

    /**
     * 동상(Chilled) 상태를 생성합니다.
     * 효과: 이동 속도 감소
     */
    public static StatusEffect chilled(int durationTicks, double slowPercent) {
        StatusEffect effect = new StatusEffect("CHILLED", "동상", StatusType.CROWD_CONTROL, durationTicks);
        effect.value = slowPercent; // 이속 감소율 (0.3 = 30%)
        return effect;
    }

    /**
     * 동결(Frozen) 상태를 생성합니다.
     * 효과: 이동/행동 불가
     */
    public static StatusEffect frozen(int durationTicks) {
        StatusEffect effect = new StatusEffect("FROZEN", "동결", StatusType.CROWD_CONTROL, durationTicks);
        return effect;
    }

    /**
     * 기절(Stunned) 상태를 생성합니다.
     * 효과: 행동 불가
     */
    public static StatusEffect stunned(int durationTicks) {
        StatusEffect effect = new StatusEffect("STUNNED", "기절", StatusType.CROWD_CONTROL, durationTicks);
        return effect;
    }

    // ===== 틱 처리 =====

    /**
     * 매 틱마다 호출됩니다.
     * 
     * @return 효과가 만료되었으면 true
     */
    public boolean tick() {
        if (durationTicks > 0) {
            durationTicks--;
        }
        return isExpired();
    }

    /**
     * 효과가 만료되었는지 확인합니다.
     */
    public boolean isExpired() {
        return durationTicks <= 0;
    }

    // ===== 중첩 및 갱신 =====

    /**
     * 효과를 갱신(리프레시)합니다.
     * 
     * @param newDuration 새 지속시간
     */
    public void refresh(int newDuration) {
        this.durationTicks = Math.max(this.durationTicks, newDuration);
        this.maxDuration = Math.max(this.maxDuration, newDuration);
        this.appliedTime = System.currentTimeMillis();
    }

    /**
     * 중첩을 추가합니다.
     * 
     * @return 성공적으로 중첩이 추가되었으면 true
     */
    public boolean addStack() {
        if (stacks < maxStacks) {
            stacks++;
            return true;
        }
        return false;
    }

    // ===== Getters & Setters =====

    public String getId() {
        return id;
    }

    public String getDisplayName() {
        return displayName;
    }

    public StatusType getType() {
        return type;
    }

    public int getDurationTicks() {
        return durationTicks;
    }

    public int getMaxDuration() {
        return maxDuration;
    }

    public double getDurationSeconds() {
        return durationTicks / 20.0;
    }

    public int getStacks() {
        return stacks;
    }

    public int getMaxStacks() {
        return maxStacks;
    }

    public StatusEffect setMaxStacks(int maxStacks) {
        this.maxStacks = maxStacks;
        return this;
    }

    public double getValue() {
        return value;
    }

    public StatusEffect setValue(double value) {
        this.value = value;
        return this;
    }

    /**
     * 중첩 수를 고려한 총 효과 수치를 반환합니다.
     */
    public double getTotalValue() {
        return value * stacks;
    }

    public long getAppliedTime() {
        return appliedTime;
    }

    public boolean isBuff() {
        return type == StatusType.BUFF;
    }

    public boolean isDebuff() {
        return type == StatusType.DEBUFF;
    }

    public boolean isCrowdControl() {
        return type == StatusType.CROWD_CONTROL;
    }

    @Override
    public String toString() {
        return String.format("StatusEffect{id='%s', stacks=%d/%d, duration=%.1fs, value=%.2f}",
                id, stacks, maxStacks, getDurationSeconds(), value);
    }
}
