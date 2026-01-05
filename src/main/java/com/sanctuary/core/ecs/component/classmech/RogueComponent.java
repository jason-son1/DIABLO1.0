package com.sanctuary.core.ecs.component.classmech;

/**
 * 도적(Rogue) 메커니즘 Component
 * 
 * 핵심 시스템:
 * - 에너지(Energy): 자동 재생, 스킬 소모
 * - 콤보 포인트(Combo Points): 기본 스킬로 적립, 핵심 스킬로 소모
 * - 주입(Imbuement): 스킬에 원소 효과 부여 (독/얼음/그림자)
 */
public class RogueComponent implements ClassMechanic {

    // 에너지 시스템
    private double energy = 100;
    private static final double MAX_ENERGY = 100;
    private static final double ENERGY_REGEN_PER_SECOND = 20;

    // 콤보 포인트 시스템
    private int comboPoints = 0;
    private static final int MAX_COMBO_POINTS = 6;

    // 주입 시스템
    public enum ImbuementType {
        NONE("없음", ""),
        POISON("독 주입", "적중 시 중독 부여"),
        COLD("냉기 주입", "적중 시 냉각/동결"),
        SHADOW("그림자 주입", "적중 시 감염 부여");

        public final String name;
        public final String effect;

        ImbuementType(String name, String effect) {
            this.name = name;
            this.effect = effect;
        }
    }

    private ImbuementType activeImbuement = ImbuementType.NONE;
    private int imbuementCharges = 0;

    @Override
    public String getClassName() {
        return "ROGUE";
    }

    @Override
    public String getResourceName() {
        return "에너지";
    }

    @Override
    public double getResource() {
        return energy;
    }

    @Override
    public double getMaxResource() {
        return MAX_ENERGY;
    }

    @Override
    public boolean consumeResource(double amount) {
        if (energy >= amount) {
            energy -= amount;
            return true;
        }
        return false;
    }

    @Override
    public void generateResource(double amount) {
        energy = Math.min(MAX_ENERGY, energy + amount);
    }

    @Override
    public void onTick() {
        // 에너지 자동 재생
        energy = Math.min(MAX_ENERGY, energy + (ENERGY_REGEN_PER_SECOND / 20.0));
    }

    @Override
    public void onSkillUse(String skillId, String category) {
        if ("BASIC".equals(category)) {
            // 기본 스킬: 콤보 포인트 생성
            addComboPoint(1);
        } else if ("CORE".equals(category)) {
            // 핵심 스킬: 콤보 포인트 소모
            // 피해량은 콤보 포인트에 비례
        }
    }

    @Override
    public void onDamageDealt(double damage, boolean isCrit, boolean isOverpower) {
        // 치명타 시 에너지 회복
        if (isCrit) {
            generateResource(5);
        }

        // 주입 충전 소모
        if (activeImbuement != ImbuementType.NONE && imbuementCharges > 0) {
            imbuementCharges--;
            if (imbuementCharges <= 0) {
                activeImbuement = ImbuementType.NONE;
            }
        }
    }

    @Override
    public void onDamageTaken(double damage) {
        // 피격 시 콤보 포인트 감소 (선택적)
    }

    @Override
    public void onKill() {
        // 적 처치 시 에너지 회복
        generateResource(10);
        addComboPoint(1);
    }

    @Override
    public void reset() {
        energy = MAX_ENERGY;
        comboPoints = 0;
        activeImbuement = ImbuementType.NONE;
        imbuementCharges = 0;
    }

    @Override
    public String getStatusDisplay() {
        StringBuilder sb = new StringBuilder();
        sb.append("§e에너지: §f").append((int) energy).append("/").append((int) MAX_ENERGY);
        sb.append(" §d콤보: §f").append(comboPoints).append("/").append(MAX_COMBO_POINTS);
        if (activeImbuement != ImbuementType.NONE) {
            sb.append(" §a[").append(activeImbuement.name).append(" x").append(imbuementCharges).append("]");
        }
        return sb.toString();
    }

    // ===== 도적 전용 메서드 =====

    public int getComboPoints() {
        return comboPoints;
    }

    public void addComboPoint(int points) {
        comboPoints = Math.min(MAX_COMBO_POINTS, comboPoints + points);
    }

    public int consumeComboPoints() {
        int consumed = comboPoints;
        comboPoints = 0;
        return consumed;
    }

    public double getComboBonus() {
        // 콤보 포인트당 15% 피해 증가
        return 1.0 + (comboPoints * 0.15);
    }

    public void activateImbuement(ImbuementType type, int charges) {
        activeImbuement = type;
        imbuementCharges = charges;
    }

    public ImbuementType getActiveImbuement() {
        return activeImbuement;
    }

    public int getImbuementCharges() {
        return imbuementCharges;
    }
}
