package com.sanctuary.combat.skill;

import com.sanctuary.core.data.DataRepository;
import com.sanctuary.core.ecs.EntityManager;
import com.sanctuary.core.ecs.SanctuaryEntity;
import com.sanctuary.core.ecs.component.IdentityComponent;

import java.util.*;
import java.util.logging.Logger;
import java.util.stream.Collectors;

/**
 * 스킬 트리 관리자
 * 스킬 포인트 투자, 리스펙, 조건 검증을 담당합니다.
 */
public class SkillTreeManager {

    private final Logger logger;
    private final EntityManager entityManager;
    private final Map<String, SkillData> skillDataMap = new HashMap<>();

    public SkillTreeManager(Logger logger, EntityManager entityManager, DataRepository dataRepository) {
        this.logger = logger;
        this.entityManager = entityManager;

        // 스킬 데이터 로드
        loadSkillData(dataRepository);
    }

    private void loadSkillData(DataRepository dataRepository) {
        // TODO: DataRepository에서 skills.json 로드
        // Collection<SkillData> skills = dataRepository.getAllSkills();
        // skills.forEach(s -> skillDataMap.put(s.getId(), s));
        logger.info("[SkillTreeManager] 스킬 데이터 로드 대기 중...");
    }

    /**
     * 스킬 데이터를 수동으로 등록합니다.
     */
    public void registerSkill(SkillData skill) {
        skillDataMap.put(skill.getId(), skill);
    }

    /**
     * 스킬 데이터를 반환합니다.
     */
    public SkillData getSkillData(String skillId) {
        return skillDataMap.get(skillId);
    }

    /**
     * 직업별 스킬 목록을 반환합니다.
     */
    public List<SkillData> getSkillsForClass(String className) {
        return skillDataMap.values().stream()
                .filter(s -> className.equalsIgnoreCase(s.getClassName()))
                .sorted(Comparator.comparingInt(SkillData::getTier))
                .collect(Collectors.toList());
    }

    /**
     * 스킬에 포인트를 투자할 수 있는지 확인합니다.
     */
    public boolean canInvestPoint(SanctuaryEntity player, String skillId) {
        SkillData skill = skillDataMap.get(skillId);
        if (skill == null) {
            return false;
        }

        SkillComponent skillComp = player.getComponent(SkillComponent.class);
        IdentityComponent identity = player.getComponent(IdentityComponent.class);

        if (skillComp == null || identity == null) {
            return false;
        }

        // 1. 사용 가능한 포인트 확인
        if (skillComp.getAvailablePoints() <= 0) {
            return false;
        }

        // 2. 최대 랭크 확인
        if (skillComp.getSkillRank(skillId) >= skill.getMaxRank()) {
            return false;
        }

        // 3. 직업 확인
        if (!skill.getClassName().equalsIgnoreCase(identity.getJob())) {
            return false;
        }

        // 4. 전제 포인트 확인
        if (skillComp.getTotalInvestedPoints() < skill.getPointsRequired()) {
            return false;
        }

        // 5. 선행 스킬 확인
        for (String prereq : skill.getPrerequisites()) {
            if (!skillComp.isSkillUnlocked(prereq)) {
                return false;
            }
        }

        return true;
    }

    /**
     * 스킬에 포인트를 투자합니다.
     */
    public InvestResult investPoint(SanctuaryEntity player, String skillId) {
        if (!canInvestPoint(player, skillId)) {
            return InvestResult.FAIL_REQUIREMENTS;
        }

        SkillData skill = skillDataMap.get(skillId);
        SkillComponent skillComp = player.getComponent(SkillComponent.class);

        if (skillComp.investPoint(skillId, skill.getMaxRank())) {
            logger.fine("[SkillTree] 스킬 투자 성공: " + skillId + " -> Rank " + skillComp.getSkillRank(skillId));
            return InvestResult.SUCCESS;
        }

        return InvestResult.FAIL_UNKNOWN;
    }

    /**
     * 스킬에서 포인트를 회수할 수 있는지 확인합니다.
     */
    public boolean canRefundPoint(SanctuaryEntity player, String skillId) {
        SkillComponent skillComp = player.getComponent(SkillComponent.class);
        if (skillComp == null) {
            return false;
        }

        // 스킬에 투자된 포인트가 있어야 함
        if (skillComp.getSkillRank(skillId) <= 0) {
            return false;
        }

        // 이 스킬을 선행으로 요구하는 다른 스킬이 해금되어 있으면 회수 불가
        for (SkillData skill : skillDataMap.values()) {
            if (skill.getPrerequisites().contains(skillId)) {
                if (skillComp.isSkillUnlocked(skill.getId())) {
                    return false;
                }
            }
        }

        return true;
    }

    /**
     * 스킬에서 포인트를 회수합니다.
     */
    public InvestResult refundPoint(SanctuaryEntity player, String skillId) {
        if (!canRefundPoint(player, skillId)) {
            return InvestResult.FAIL_DEPENDENT;
        }

        SkillComponent skillComp = player.getComponent(SkillComponent.class);
        if (skillComp.refundPoint(skillId)) {
            logger.fine("[SkillTree] 스킬 회수 성공: " + skillId);
            return InvestResult.SUCCESS;
        }

        return InvestResult.FAIL_UNKNOWN;
    }

    /**
     * 스킬 트리 전체를 리스펙합니다.
     */
    public void respec(SanctuaryEntity player) {
        SkillComponent skillComp = player.getComponent(SkillComponent.class);
        if (skillComp != null) {
            skillComp.resetSkillTree();
            logger.info("[SkillTree] 스킬 트리 리스펙 완료");
        }
    }

    /**
     * 플레이어 레벨업 시 스킬 포인트를 부여합니다.
     */
    public void onLevelUp(SanctuaryEntity player, int newLevel) {
        SkillComponent skillComp = player.getComponent(SkillComponent.class);
        if (skillComp == null) {
            skillComp = new SkillComponent();
            player.attach(skillComp);
        }

        // 레벨당 1포인트 (50레벨까지 기본 50포인트)
        skillComp.addPoints(1);
    }

    /**
     * 투자 결과
     */
    public enum InvestResult {
        SUCCESS,
        FAIL_NO_POINTS,
        FAIL_MAX_RANK,
        FAIL_WRONG_CLASS,
        FAIL_REQUIREMENTS,
        FAIL_DEPENDENT,
        FAIL_UNKNOWN
    }
}
