package com.sanctuary.core.ecs.component.classmech;

import java.util.EnumSet;
import java.util.Set;

/**
 * 원소술사(Sorcerer) 메커니즘 Component
 * 
 * 핵심 시스템:
 * - 마나(Mana): 자동 재생, 스킬 소모
 * - 마법부여(Enchantment): 스킬 6개 중 3개에 패시브 효과 부여
 * - 장벽(Barrier): 피해 흡수 보호막
 */
public class SorcererComponent implements ClassMechanic {

    // 마나 시스템
    private double mana = 100;
    private static final double MAX_MANA = 100;
    private static final double MANA_REGEN_PER_SECOND = 10;

    // 마법부여 시스템
    public enum EnchantmentSlot {
        SLOT_1, SLOT_2, SLOT_3
    }

    private final String[] enchantedSkills = new String[3];
    private final Set<EnchantmentSlot> activeSlots = EnumSet.noneOf(EnchantmentSlot.class);

    // 장벽 시스템
    private double barrier = 0;
    private static final double MAX_BARRIER_PERCENT = 0.5; // 최대 체력의 50%

    // 크래킹 에너지 (치명타 시 슬로우/동결)
    private int cracklingEnergyStacks = 0;

    @Override
    public String getClassName() {
        return "SORCERER";
    }

    @Override
    public String getResourceName() {
        return "마나";
    }

    @Override
    public double getResource() {
        return mana;
    }

    @Override
    public double getMaxResource() {
        return MAX_MANA;
    }

    @Override
    public boolean consumeResource(double amount) {
        if (mana >= amount) {
            mana -= amount;
            return true;
        }
        return false;
    }

    @Override
    public void generateResource(double amount) {
        mana = Math.min(MAX_MANA, mana + amount);
    }

    @Override
    public void onTick() {
        // 마나 자동 재생
        mana = Math.min(MAX_MANA, mana + (MANA_REGEN_PER_SECOND / 20.0));

        // 크래킹 에너지 발동 (주변 적에게 자동 피해)
        if (cracklingEnergyStacks > 0) {
            // TODO: 주변 적에게 번개 피해 발동
        }
    }

    @Override
    public void onSkillUse(String skillId, String category) {
        // 마법부여된 스킬은 패시브 효과 발동
        if (isEnchanted(skillId)) {
            triggerEnchantmentEffect(skillId);
        }
    }

    @Override
    public void onDamageDealt(double damage, boolean isCrit, boolean isOverpower) {
        // 치명타 시 크래킹 에너지 생성
        if (isCrit) {
            cracklingEnergyStacks = Math.min(5, cracklingEnergyStacks + 1);
        }
    }

    @Override
    public void onDamageTaken(double damage) {
        // 장벽이 먼저 피해 흡수
        if (barrier > 0) {
            if (barrier >= damage) {
                barrier -= damage;
            } else {
                barrier = 0;
                // 남은 피해는 체력에 적용됨
            }
        }
    }

    @Override
    public void onKill() {
        // 적 처치 시 마나 회복
        generateResource(5);
    }

    @Override
    public void reset() {
        mana = MAX_MANA;
        barrier = 0;
        cracklingEnergyStacks = 0;
        for (int i = 0; i < enchantedSkills.length; i++) {
            enchantedSkills[i] = null;
        }
        activeSlots.clear();
    }

    @Override
    public String getStatusDisplay() {
        StringBuilder sb = new StringBuilder();
        sb.append("§9마나: §f").append((int) mana).append("/").append((int) MAX_MANA);
        if (barrier > 0) {
            sb.append(" §b장벽: §f").append((int) barrier);
        }
        if (cracklingEnergyStacks > 0) {
            sb.append(" §e⚡x").append(cracklingEnergyStacks);
        }
        return sb.toString();
    }

    // ===== 원소술사 전용 메서드 =====

    public boolean setEnchantment(EnchantmentSlot slot, String skillId) {
        int index = slot.ordinal();
        enchantedSkills[index] = skillId;
        activeSlots.add(slot);
        return true;
    }

    public String getEnchantment(EnchantmentSlot slot) {
        return enchantedSkills[slot.ordinal()];
    }

    public boolean isEnchanted(String skillId) {
        for (String enchanted : enchantedSkills) {
            if (skillId.equals(enchanted)) {
                return true;
            }
        }
        return false;
    }

    private void triggerEnchantmentEffect(String skillId) {
        // 마법부여 패시브 효과 발동
        // 예: FIREBALL → "적 처치 시 폭발"
        // TODO: 스킬별 마법부여 효과 구현
    }

    public void addBarrier(double amount, double maxHealth) {
        double maxBarrier = maxHealth * MAX_BARRIER_PERCENT;
        barrier = Math.min(maxBarrier, barrier + amount);
    }

    public double getBarrier() {
        return barrier;
    }

    public int getCracklingEnergyStacks() {
        return cracklingEnergyStacks;
    }

    public void consumeCracklingEnergy() {
        if (cracklingEnergyStacks > 0) {
            cracklingEnergyStacks--;
        }
    }
}
