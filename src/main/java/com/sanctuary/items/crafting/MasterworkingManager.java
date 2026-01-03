package com.sanctuary.items.crafting;

import com.sanctuary.items.model.AffixInstance;
import com.sanctuary.items.model.ItemRarity;
import com.sanctuary.items.model.MasterworkingData;
import com.sanctuary.items.model.RpgItemData;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;
import java.util.logging.Logger;

/**
 * 명품화(Masterworking) 시스템을 관리합니다.
 * 아이템 성능을 12단계까지 강화합니다.
 */
public class MasterworkingManager {

    private final Logger logger;

    // 일반 단계 강화 배율 (5%)
    private static final double NORMAL_BONUS = 1.05;

    // 크리티컬 단계 강화 배율 (25%)
    private static final double CRITICAL_BONUS = 1.25;

    public MasterworkingManager(Logger logger) {
        this.logger = logger;
    }

    /**
     * 명품화 결과를 저장하는 레코드입니다.
     */
    public record MasterworkResult(
            boolean success,
            String message,
            int newRank,
            boolean isCritical,
            int criticalAffixIndex) {
    }

    /**
     * 명품화가 가능한지 확인합니다.
     *
     * @param item 대상 아이템
     * @return 가능 여부
     */
    public boolean canUpgrade(RpgItemData item) {
        // 전설 등급 이상만 가능
        if (item.getRarity().getTier() < ItemRarity.LEGENDARY.getTier()) {
            return false;
        }

        MasterworkingData masterworking = item.getMasterworking();
        return masterworking.canUpgrade();
    }

    /**
     * 명품화 강화를 시도합니다.
     *
     * @param item 대상 아이템
     * @return 결과
     */
    public MasterworkResult attemptUpgrade(RpgItemData item) {
        // 1. 검증
        if (!canUpgrade(item)) {
            return new MasterworkResult(false, "명품화가 불가능한 아이템입니다.", 0, false, -1);
        }

        MasterworkingData masterworking = item.getMasterworking();

        // 2. 강화 가능한 어픽스 목록 수집
        List<AffixInstance> allAffixes = getAllAffixes(item);
        if (allAffixes.isEmpty()) {
            return new MasterworkResult(false, "강화 가능한 어픽스가 없습니다.", 0, false, -1);
        }

        // 3. 다음 랭크가 크리티컬인지 확인
        boolean willBeCritical = masterworking.isNextRankCritical();

        // 4. 랭크 업그레이드
        masterworking.upgrade();
        int newRank = masterworking.getRank();

        // 5. 어픽스 강화 적용
        int criticalIndex = -1;
        if (willBeCritical) {
            // 크리티컬 단계: 랜덤 1개 어픽스 25% 증가
            criticalIndex = ThreadLocalRandom.current().nextInt(allAffixes.size());
            AffixInstance critAffix = allAffixes.get(criticalIndex);
            double newValue = critAffix.getValue() * CRITICAL_BONUS;
            critAffix.setValue(newValue);
            masterworking.addCritical(criticalIndex);

            logger.info("[Masterworking] 크리티컬! " + critAffix.getStatKey() + " -> " +
                    String.format("%.1f", newValue) + " (랭크 " + newRank + ")");
        } else {
            // 일반 단계: 모든 어픽스 5% 증가
            for (AffixInstance affix : allAffixes) {
                double newValue = affix.getValue() * NORMAL_BONUS;
                affix.setValue(newValue);
            }

            logger.info("[Masterworking] 강화 성공 (랭크 " + newRank + ")");
        }

        String message = willBeCritical ? "크리티컬 강화!" : "강화 성공!";
        return new MasterworkResult(true, message, newRank, willBeCritical, criticalIndex);
    }

    /**
     * 명품화를 초기화합니다.
     *
     * @param item 대상 아이템
     * @return 성공 여부
     */
    public boolean reset(RpgItemData item) {
        MasterworkingData masterworking = item.getMasterworking();
        if (masterworking.getRank() == 0) {
            return false;
        }

        // 주의: 재료는 반환되지 않음 (디아블로 4 정책)
        masterworking.reset();
        logger.info("[Masterworking] 명품화 초기화됨");
        return true;
    }

    /**
     * 아이템의 모든 강화 가능한 어픽스를 수집합니다.
     */
    private List<AffixInstance> getAllAffixes(RpgItemData item) {
        List<AffixInstance> result = new ArrayList<>();

        // 명시적 어픽스
        result.addAll(item.getExplicitAffixes());

        // 담금질 어픽스
        if (item.getTempering() != null) {
            if (item.getTempering().getSlot1() != null) {
                result.add(item.getTempering().getSlot1());
            }
            if (item.getTempering().getSlot2() != null) {
                result.add(item.getTempering().getSlot2());
            }
        }

        return result;
    }

    /**
     * 현재 랭크에 따른 색상 접두사를 반환합니다.
     */
    public String getRankDisplayName(int rank) {
        String color;
        if (rank <= 4) {
            color = "§9"; // 파랑
        } else if (rank <= 8) {
            color = "§e"; // 노랑
        } else {
            color = "§6"; // 주황
        }
        return color + "★" + rank;
    }
}
