package com.sanctuary.core.ecs.component.classmech;

/**
 * 야만용사(Barbarian) 메커니즘 Component
 * 
 * 핵심 시스템:
 * - 분노(Fury): 기본 스킬로 생성, 핵심 스킬로 소모
 * - 무기 기예(Arsenal): 4종 무기 마스터리
 * - 광폭(Berserking): 공격 속도/이동 속도 증가 버프
 */
public class BarbarianComponent implements ClassMechanic {

    // 분노 시스템
    private double fury = 0;
    private static final double MAX_FURY = 100;
    private static final double FURY_DECAY_PER_SECOND = 5;

    // 광폭 상태
    private boolean berserking = false;
    private int berserkTicks = 0;

    // 무기 기예 (무기 타입별 마스터리)
    public enum WeaponMastery {
        TWO_HANDED_SWORD("양손검", "출혈 피해 증가"),
        TWO_HANDED_AXE("양손도끼", "취약 적중 피해 증가"),
        TWO_HANDED_MACE("양손철퇴", "제압 피해 증가"),
        DUAL_WIELD("쌍수 무기", "공격 속도 증가");

        public final String name;
        public final String bonus;

        WeaponMastery(String name, String bonus) {
            this.name = name;
            this.bonus = bonus;
        }
    }

    private WeaponMastery currentMastery = WeaponMastery.TWO_HANDED_SWORD;
    private int masteryExperience = 0;

    @Override
    public String getClassName() {
        return "BARBARIAN";
    }

    @Override
    public String getResourceName() {
        return "분노";
    }

    @Override
    public double getResource() {
        return fury;
    }

    @Override
    public double getMaxResource() {
        return MAX_FURY;
    }

    @Override
    public boolean consumeResource(double amount) {
        if (fury >= amount) {
            fury -= amount;
            return true;
        }
        return false;
    }

    @Override
    public void generateResource(double amount) {
        fury = Math.min(MAX_FURY, fury + amount);
    }

    @Override
    public void onTick() {
        // 분노 자연 감소 (20틱 = 1초 기준으로 1/20씩)
        if (fury > 0) {
            fury = Math.max(0, fury - (FURY_DECAY_PER_SECOND / 20.0));
        }

        // 광폭 지속시간 감소
        if (berserking && berserkTicks > 0) {
            berserkTicks--;
            if (berserkTicks <= 0) {
                berserking = false;
            }
        }
    }

    @Override
    public void onSkillUse(String skillId, String category) {
        // 핵심 스킬 사용 시 광폭 갱신 가능
        if ("CORE".equals(category) && fury >= 50) {
            // 분노 50 이상 시 광폭 발동 확률
        }
    }

    @Override
    public void onDamageDealt(double damage, boolean isCrit, boolean isOverpower) {
        // 치명타 시 분노 추가 생성
        if (isCrit) {
            generateResource(5);
        }

        // 제압 시 광폭 연장
        if (isOverpower && berserking) {
            berserkTicks = Math.min(berserkTicks + 20, 100); // 최대 5초
        }
    }

    @Override
    public void onDamageTaken(double damage) {
        // 피격 시 분노 소량 생성
        generateResource(2);
    }

    @Override
    public void onKill() {
        // 적 처치 시 광폭 연장
        if (berserking) {
            berserkTicks += 20; // 1초 연장
        }
        generateResource(10);
    }

    @Override
    public void reset() {
        fury = 0;
        berserking = false;
        berserkTicks = 0;
    }

    @Override
    public String getStatusDisplay() {
        StringBuilder sb = new StringBuilder();
        sb.append("§c분노: §f").append((int) fury).append("/").append((int) MAX_FURY);
        if (berserking) {
            sb.append(" §6[광폭 ").append(berserkTicks / 20).append("초]");
        }
        sb.append(" §7무기: §f").append(currentMastery.name);
        return sb.toString();
    }

    // ===== 야만용사 전용 메서드 =====

    public void activateBerserk(int durationTicks) {
        berserking = true;
        berserkTicks = durationTicks;
    }

    public boolean isBerserking() {
        return berserking;
    }

    public WeaponMastery getCurrentMastery() {
        return currentMastery;
    }

    public void setCurrentMastery(WeaponMastery mastery) {
        this.currentMastery = mastery;
    }

    public void addMasteryExperience(int exp) {
        masteryExperience += exp;
    }

    public int getMasteryExperience() {
        return masteryExperience;
    }
}
