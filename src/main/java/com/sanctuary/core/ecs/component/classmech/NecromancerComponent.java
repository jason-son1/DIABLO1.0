package com.sanctuary.core.ecs.component.classmech;

/**
 * 강령술사(Necromancer) 메커니즘 Component
 * 
 * 핵심 시스템:
 * - 정수(Essence): 기본 스킬로 생성, 핵심 스킬로 소모
 * - 시체(Corpse): 주변 시체를 활용한 스킬
 * - 소환수(Minions): 해골/골렘 소환 및 관리
 * - 뼈/피(Book of the Dead): 소환수 커스터마이징
 */
public class NecromancerComponent implements ClassMechanic {

    // 정수 시스템
    private double essence = 0;
    private static final double MAX_ESSENCE = 100;

    // 시체 시스템
    private int nearbyCorpses = 0;
    private static final int MAX_CORPSES = 10;

    // 소환수 시스템
    public enum MinionType {
        SKELETON_WARRIOR("해골 전사", 4),
        SKELETON_MAGE("해골 마법사", 4),
        GOLEM("골렘", 1);

        public final String name;
        public final int maxCount;

        MinionType(String name, int maxCount) {
            this.name = name;
            this.maxCount = maxCount;
        }
    }

    private int skeletonWarriors = 0;
    private int skeletonMages = 0;
    private boolean hasGolem = false;

    // 뼈/피 책 선택
    public enum BookOfDeadChoice {
        SACRIFICE("희생", "소환수 대신 직접 강화"),
        REAPERS("수확자", "공격 시 회복"),
        DEFENDERS("수호자", "방어 강화");

        public final String name;
        public final String effect;

        BookOfDeadChoice(String name, String effect) {
            this.name = name;
            this.effect = effect;
        }
    }

    private BookOfDeadChoice skeletonChoice = BookOfDeadChoice.REAPERS;
    private BookOfDeadChoice golemChoice = BookOfDeadChoice.DEFENDERS;

    @Override
    public String getClassName() {
        return "NECROMANCER";
    }

    @Override
    public String getResourceName() {
        return "정수";
    }

    @Override
    public double getResource() {
        return essence;
    }

    @Override
    public double getMaxResource() {
        return MAX_ESSENCE;
    }

    @Override
    public boolean consumeResource(double amount) {
        if (essence >= amount) {
            essence -= amount;
            return true;
        }
        return false;
    }

    @Override
    public void generateResource(double amount) {
        essence = Math.min(MAX_ESSENCE, essence + amount);
    }

    @Override
    public void onTick() {
        // 소환수가 피해 시 정수 소량 생성
        // 시체 부패 시스템 (선택적)
    }

    @Override
    public void onSkillUse(String skillId, String category) {
        if ("CORPSE".equals(category)) {
            // 시체 스킬 사용 시 시체 소모
            if (nearbyCorpses > 0) {
                nearbyCorpses--;
            }
        }
    }

    @Override
    public void onDamageDealt(double damage, boolean isCrit, boolean isOverpower) {
        // 피해 시 정수 생성 (소환수 피해 포함)
    }

    @Override
    public void onDamageTaken(double damage) {
        // 피격 시 뼈 갑옷 스택 소모 등
    }

    @Override
    public void onKill() {
        // 적 처치 시 시체 생성
        addCorpse(1);
        generateResource(10);
    }

    @Override
    public void reset() {
        essence = 0;
        nearbyCorpses = 0;
        skeletonWarriors = 0;
        skeletonMages = 0;
        hasGolem = false;
    }

    @Override
    public String getStatusDisplay() {
        StringBuilder sb = new StringBuilder();
        sb.append("§5정수: §f").append((int) essence).append("/").append((int) MAX_ESSENCE);
        sb.append(" §7시체: §f").append(nearbyCorpses);

        int totalMinions = skeletonWarriors + skeletonMages + (hasGolem ? 1 : 0);
        if (totalMinions > 0) {
            sb.append(" §8소환: §f").append(totalMinions);
        }
        return sb.toString();
    }

    // ===== 강령술사 전용 메서드 =====

    public int getNearbyCorpses() {
        return nearbyCorpses;
    }

    public void addCorpse(int count) {
        nearbyCorpses = Math.min(MAX_CORPSES, nearbyCorpses + count);
    }

    public boolean consumeCorpse() {
        if (nearbyCorpses > 0) {
            nearbyCorpses--;
            return true;
        }
        return false;
    }

    public void summonSkeleton(boolean isMage) {
        if (isMage) {
            if (skeletonMages < MinionType.SKELETON_MAGE.maxCount) {
                skeletonMages++;
            }
        } else {
            if (skeletonWarriors < MinionType.SKELETON_WARRIOR.maxCount) {
                skeletonWarriors++;
            }
        }
    }

    public void summonGolem() {
        hasGolem = true;
    }

    public int getSkeletonWarriors() {
        return skeletonWarriors;
    }

    public int getSkeletonMages() {
        return skeletonMages;
    }

    public boolean hasGolem() {
        return hasGolem;
    }

    public int getTotalMinions() {
        return skeletonWarriors + skeletonMages + (hasGolem ? 1 : 0);
    }

    public void setBookChoice(MinionType type, BookOfDeadChoice choice) {
        if (type == MinionType.GOLEM) {
            golemChoice = choice;
        } else {
            skeletonChoice = choice;
        }
    }

    public boolean isSacrificed(MinionType type) {
        if (type == MinionType.GOLEM) {
            return golemChoice == BookOfDeadChoice.SACRIFICE;
        }
        return skeletonChoice == BookOfDeadChoice.SACRIFICE;
    }
}
