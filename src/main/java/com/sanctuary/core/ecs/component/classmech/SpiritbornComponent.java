package com.sanctuary.core.ecs.component.classmech;

/**
 * 혼령사(Spiritborn) 메커니즘 Component
 * 
 * 핵심 시스템:
 * - 에너지(Vigor): 자동 재생, 스킬 소모
 * - 정령 수호자(Spirit Guardians): 4종 정령 중 1종 활성화
 * - 화신(Incarnate): 정령 능력 극대화 궁극기
 * 
 * 4종 정령:
 * - 독수리(Eagle): 이동/공속, 번개 피해
 * - 재규어(Jaguar): 돌진/연속타, 화염 피해
 * - 고릴라(Gorilla): 방어/보강, 물리 피해
 * - 지네(Centipede): 독/지속피해, 독 피해
 */
public class SpiritbornComponent implements ClassMechanic {

    // 에너지 시스템
    private double vigor = 50;
    private static final double MAX_VIGOR = 100;
    private static final double VIGOR_REGEN_PER_SECOND = 8;

    // 정령 시스템
    public enum SpiritGuardian {
        EAGLE("독수리", "LIGHTNING", "공격/이동 속도 증가", 0xFFFF00),
        JAGUAR("재규어", "FIRE", "연속 공격 강화", 0xFF6600),
        GORILLA("고릴라", "PHYSICAL", "방어력/보강 강화", 0x808080),
        CENTIPEDE("지네", "POISON", "지속 피해 강화", 0x00FF00);

        public final String name;
        public final String damageType;
        public final String bonus;
        public final int color;

        SpiritGuardian(String name, String damageType, String bonus, int color) {
            this.name = name;
            this.damageType = damageType;
            this.bonus = bonus;
            this.color = color;
        }
    }

    private SpiritGuardian primaryGuardian = SpiritGuardian.EAGLE;
    private SpiritGuardian secondaryGuardian = SpiritGuardian.JAGUAR;

    // 화신 상태
    private boolean incarnateActive = false;
    private int incarnateDurationTicks = 0;

    // 정령 공명 스택
    private int resonanceStacks = 0;
    private static final int MAX_RESONANCE = 5;

    @Override
    public String getClassName() {
        return "SPIRITBORN";
    }

    @Override
    public String getResourceName() {
        return "활력";
    }

    @Override
    public double getResource() {
        return vigor;
    }

    @Override
    public double getMaxResource() {
        return MAX_VIGOR;
    }

    @Override
    public boolean consumeResource(double amount) {
        if (vigor >= amount) {
            vigor -= amount;
            return true;
        }
        return false;
    }

    @Override
    public void generateResource(double amount) {
        vigor = Math.min(MAX_VIGOR, vigor + amount);
    }

    @Override
    public void onTick() {
        // 에너지 자동 재생
        vigor = Math.min(MAX_VIGOR, vigor + (VIGOR_REGEN_PER_SECOND / 20.0));

        // 화신 지속시간 감소
        if (incarnateActive && incarnateDurationTicks > 0) {
            incarnateDurationTicks--;
            if (incarnateDurationTicks <= 0) {
                incarnateActive = false;
            }
        }
    }

    @Override
    public void onSkillUse(String skillId, String category) {
        // 정령 스킬 사용 시 공명 스택 획득
        if (skillId.contains(primaryGuardian.name().toUpperCase())) {
            addResonance(1);
        }
    }

    @Override
    public void onDamageDealt(double damage, boolean isCrit, boolean isOverpower) {
        // 독수리: 치명타 시 에너지 회복
        if (primaryGuardian == SpiritGuardian.EAGLE && isCrit) {
            generateResource(5);
        }

        // 재규어: 연속 피해 시 공명 추가
        if (primaryGuardian == SpiritGuardian.JAGUAR) {
            addResonance(1);
        }
    }

    @Override
    public void onDamageTaken(double damage) {
        // 고릴라: 피격 시 보강 생성
        if (primaryGuardian == SpiritGuardian.GORILLA) {
            // 보강 생성은 StateComponent에서 처리
        }
    }

    @Override
    public void onKill() {
        // 적 처치 시 에너지 회복 및 공명 추가
        generateResource(10);
        addResonance(2);

        // 지네: 처치 시 독 확산
        if (primaryGuardian == SpiritGuardian.CENTIPEDE) {
            // 주변 적에게 독 확산
        }
    }

    @Override
    public void reset() {
        vigor = 50;
        incarnateActive = false;
        incarnateDurationTicks = 0;
        resonanceStacks = 0;
    }

    @Override
    public String getStatusDisplay() {
        StringBuilder sb = new StringBuilder();
        sb.append("§b활력: §f").append((int) vigor).append("/").append((int) MAX_VIGOR);
        sb.append(" §7정령: §f").append(primaryGuardian.name);

        if (resonanceStacks > 0) {
            sb.append(" §d공명: §f").append(resonanceStacks);
        }

        if (incarnateActive) {
            sb.append(" §6[화신 ").append(incarnateDurationTicks / 20).append("초]");
        }
        return sb.toString();
    }

    // ===== 혼령사 전용 메서드 =====

    public SpiritGuardian getPrimaryGuardian() {
        return primaryGuardian;
    }

    public void setPrimaryGuardian(SpiritGuardian guardian) {
        this.primaryGuardian = guardian;
    }

    public SpiritGuardian getSecondaryGuardian() {
        return secondaryGuardian;
    }

    public void setSecondaryGuardian(SpiritGuardian guardian) {
        this.secondaryGuardian = guardian;
    }

    public void activateIncarnate(int durationTicks) {
        incarnateActive = true;
        incarnateDurationTicks = durationTicks;
    }

    public boolean isIncarnateActive() {
        return incarnateActive;
    }

    public void addResonance(int stacks) {
        resonanceStacks = Math.min(MAX_RESONANCE, resonanceStacks + stacks);
    }

    public int getResonanceStacks() {
        return resonanceStacks;
    }

    public int consumeResonance() {
        int consumed = resonanceStacks;
        resonanceStacks = 0;
        return consumed;
    }

    public double getResonanceBonus() {
        // 공명 스택당 10% 피해 증가
        return 1.0 + (resonanceStacks * 0.10);
    }

    public String getActiveDamageType() {
        return incarnateActive ? primaryGuardian.damageType : "PHYSICAL";
    }
}
