package com.sanctuary.items.model;

import java.util.ArrayList;
import java.util.List;

/**
 * 명품화(Masterworking) 상태를 저장하는 클래스입니다.
 */
public class MasterworkingData {

    private int rank; // 현재 랭크 (0~12)
    private List<Integer> criticalIndices; // 크리티컬 적중한 어픽스 인덱스 목록

    public MasterworkingData() {
        this.rank = 0;
        this.criticalIndices = new ArrayList<>();
    }

    /**
     * 현재 랭크가 크리티컬 단계인지 확인합니다.
     * 크리티컬 단계: 4, 8, 12
     */
    public static boolean isCriticalRank(int rank) {
        return rank == 4 || rank == 8 || rank == 12;
    }

    /**
     * 다음 랭크가 크리티컬 단계인지 확인합니다.
     */
    public boolean isNextRankCritical() {
        return isCriticalRank(rank + 1);
    }

    /**
     * 강화가 가능한지 확인합니다.
     */
    public boolean canUpgrade() {
        return rank < 12;
    }

    /**
     * 랭크를 1 증가시킵니다.
     * 
     * @return 크리티컬 단계에 도달했으면 true
     */
    public boolean upgrade() {
        if (rank >= 12)
            return false;
        rank++;
        return isCriticalRank(rank);
    }

    /**
     * 크리티컬 적중 인덱스를 추가합니다.
     * 
     * @param affixIndex 어픽스 인덱스
     */
    public void addCritical(int affixIndex) {
        criticalIndices.add(affixIndex);
    }

    /**
     * 해당 인덱스의 어픽스가 크리티컬 적중했는지 확인합니다.
     */
    public boolean hasCritical(int affixIndex) {
        return criticalIndices.contains(affixIndex);
    }

    /**
     * 해당 인덱스의 어픽스에 크리티컬이 몇 번 적중했는지 반환합니다.
     */
    public int getCriticalCount(int affixIndex) {
        return (int) criticalIndices.stream().filter(i -> i == affixIndex).count();
    }

    /**
     * 랭크를 0으로 초기화합니다.
     */
    public void reset() {
        this.rank = 0;
        this.criticalIndices.clear();
    }

    // ===== Getters & Setters =====

    public int getRank() {
        return rank;
    }

    public void setRank(int rank) {
        this.rank = Math.max(0, Math.min(12, rank));
    }

    public List<Integer> getCriticalIndices() {
        return criticalIndices;
    }

    public void setCriticalIndices(List<Integer> criticalIndices) {
        this.criticalIndices = criticalIndices;
    }

    /**
     * 현재 랭크에 따른 색상 코드를 반환합니다.
     * 4이하=파랑, 8이하=노랑, 12이하=주황
     */
    public String getRankColor() {
        if (rank <= 4)
            return "§9";
        if (rank <= 8)
            return "§e";
        return "§6";
    }
}
