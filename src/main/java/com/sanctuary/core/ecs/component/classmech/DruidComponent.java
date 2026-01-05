package com.sanctuary.core.ecs.component.classmech;

/**
 * 드루이드(Druid) 메커니즘 Component
 * 
 * 핵심 시스템:
 * - 영혼(Spirit): 기본 스킬로 생성, 핵심 스킬로 소모
 * - 변신(Shapeshifting): 늑대인간/곰 변신
 * - 영혼 은총(Spirit Boons): 4종 영혼으로부터 패시브 선택
 */
public class DruidComponent implements ClassMechanic {

    // 영혼 시스템
    private double spirit = 0;
    private static final double MAX_SPIRIT = 100;

    // 변신 시스템
    public enum ShapeForm {
        HUMAN("인간", "기본 형태"),
        WEREWOLF("늑대인간", "공격 속도 15% 증가"),
        WEREBEAR("곰", "받는 피해 20% 감소");

        public final String name;
        public final String bonus;

        ShapeForm(String name, String bonus) {
            this.name = name;
            this.bonus = bonus;
        }
    }

    private ShapeForm currentForm = ShapeForm.HUMAN;
    private int formDurationTicks = 0;

    // 영혼 은총 시스템
    public enum SpiritAnimal {
        DEER("사슴", "방어 강화"),
        EAGLE("독수리", "치명타 강화"),
        WOLF("늑대", "동료 강화"),
        SNAKE("뱀", "피해 강화");

        public final String name;
        public final String category;

        SpiritAnimal(String name, String category) {
            this.name = name;
            this.category = category;
        }
    }

    // 각 영혼에서 선택한 은총 (0-3 인덱스)
    private final int[] selectedBoons = { -1, -1, -1, -1 };
    private SpiritAnimal boonedAnimal = null; // 추가 은총 받는 영혼

    @Override
    public String getClassName() {
        return "DRUID";
    }

    @Override
    public String getResourceName() {
        return "영혼";
    }

    @Override
    public double getResource() {
        return spirit;
    }

    @Override
    public double getMaxResource() {
        return MAX_SPIRIT;
    }

    @Override
    public boolean consumeResource(double amount) {
        if (spirit >= amount) {
            spirit -= amount;
            return true;
        }
        return false;
    }

    @Override
    public void generateResource(double amount) {
        spirit = Math.min(MAX_SPIRIT, spirit + amount);
    }

    @Override
    public void onTick() {
        // 변신 지속시간 감소
        if (currentForm != ShapeForm.HUMAN && formDurationTicks > 0) {
            formDurationTicks--;
            if (formDurationTicks <= 0) {
                currentForm = ShapeForm.HUMAN;
            }
        }
    }

    @Override
    public void onSkillUse(String skillId, String category) {
        // 변신 스킬 사용 시 해당 형태로 변신
        if (skillId.contains("PULVERIZE") || skillId.contains("MAUL")) {
            shapeshift(ShapeForm.WEREBEAR, 100);
        } else if (skillId.contains("SHRED") || skillId.contains("RABIES")) {
            shapeshift(ShapeForm.WEREWOLF, 100);
        }
    }

    @Override
    public void onDamageDealt(double damage, boolean isCrit, boolean isOverpower) {
        // 늑대 형태: 치명타 시 영혼 생성 증가
        if (currentForm == ShapeForm.WEREWOLF && isCrit) {
            generateResource(5);
        }
    }

    @Override
    public void onDamageTaken(double damage) {
        // 곰 형태: 피해 감소는 외부에서 처리
    }

    @Override
    public void onKill() {
        // 적 처치 시 영혼 회복
        generateResource(15);
        // 변신 지속시간 연장
        if (currentForm != ShapeForm.HUMAN) {
            formDurationTicks = Math.min(formDurationTicks + 40, 200);
        }
    }

    @Override
    public void reset() {
        spirit = 0;
        currentForm = ShapeForm.HUMAN;
        formDurationTicks = 0;
    }

    @Override
    public String getStatusDisplay() {
        StringBuilder sb = new StringBuilder();
        sb.append("§a영혼: §f").append((int) spirit).append("/").append((int) MAX_SPIRIT);
        if (currentForm != ShapeForm.HUMAN) {
            sb.append(" §6[").append(currentForm.name);
            if (formDurationTicks > 0) {
                sb.append(" ").append(formDurationTicks / 20).append("초");
            }
            sb.append("]");
        }
        return sb.toString();
    }

    // ===== 드루이드 전용 메서드 =====

    public void shapeshift(ShapeForm form, int durationTicks) {
        currentForm = form;
        formDurationTicks = durationTicks;
    }

    public ShapeForm getCurrentForm() {
        return currentForm;
    }

    public boolean isShapeshifted() {
        return currentForm != ShapeForm.HUMAN;
    }

    public void selectBoon(SpiritAnimal animal, int boonIndex) {
        selectedBoons[animal.ordinal()] = boonIndex;
    }

    public int getSelectedBoon(SpiritAnimal animal) {
        return selectedBoons[animal.ordinal()];
    }

    public void setBoonedAnimal(SpiritAnimal animal) {
        this.boonedAnimal = animal;
    }

    public SpiritAnimal getBoonedAnimal() {
        return boonedAnimal;
    }

    public double getDamageReduction() {
        return currentForm == ShapeForm.WEREBEAR ? 0.20 : 0.0;
    }

    public double getAttackSpeedBonus() {
        return currentForm == ShapeForm.WEREWOLF ? 0.15 : 0.0;
    }
}
