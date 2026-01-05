package com.sanctuary.items.model;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * RPG 아이템의 모든 데이터를 담는 POJO 클래스입니다.
 * 이 객체는 PDC에 JSON 형태로 직렬화되어 저장됩니다.
 * 
 * 디아블로 IV의 아이템 구조를 따릅니다:
 * - 기본 스탯 (베이스 아이템에서 상속)
 * - 암시적 어픽스 (Implicit - 무기 고유)
 * - 명시적 어픽스 (Explicit - 랜덤)
 * - 담금질/명품화 상태
 * - 전설 위상 (Aspect)
 */
public class RpgItemData {

    // ===== 기본 정보 =====
    private String uuid; // 아이템 고유 ID (복제 방지)
    private String templateId; // 베이스 아이템 ID (예: "ancestral_sword")
    private String displayName; // 표시 이름
    private ItemRarity rarity; // 희귀도
    private int itemPower; // 아이템 위력 (어픽스 범위 결정)
    private int requiredLevel; // 착용 요구 레벨

    // ===== 어픽스 =====
    private List<AffixInstance> implicitAffixes; // 암시적 어픽스 (무기 고유)
    private List<AffixInstance> explicitAffixes; // 명시적 어픽스 (랜덤)

    // ===== 제작 상태 =====
    private TemperingData tempering; // 담금질 상태
    private MasterworkingData masterworking; // 명품화 상태

    // ===== 전설 위상 =====
    private String aspectId; // 위상 ID
    private double aspectValue; // 위상 수치 (단일 값, 하위 호환)
    private Map<String, Double> aspectValues; // 위상 수치 (Map 형태)

    // ===== 아이템 메타 =====
    private String slotType; // 장착 슬롯 (WEAPON, CHESTPLATE, RING 등)
    private String itemId; // 아이템 식별자 (AspectManager에서 사용)

    // ===== 유니크 스크립트 (고유 아이템 효과) =====
    private String onEquipScript; // 장착 시 실행 Lua 함수명
    private String onUnequipScript; // 해제 시 실행 Lua 함수명
    private String onHitScript; // 피해 적중 시 실행 Lua 함수명
    private String onTakeDamageScript; // 피격 시 실행 Lua 함수명
    private String onKillScript; // 처치 시 실행 Lua 함수명
    private String uniqueEffectId; // 고유 효과 ID (선택적)

    // ===== 소켓 =====
    private int socketCount; // 소켓 개수
    private List<String> socketedGems; // 박힌 보석 ID 목록

    // ===== 메타 정보 =====
    private long createdAt; // 생성 시각
    private String droppedFrom; // 드롭 출처 (예: "Lilith", "World Boss")

    public RpgItemData() {
        this.uuid = UUID.randomUUID().toString();
        this.rarity = ItemRarity.COMMON;
        this.itemPower = 1;
        this.implicitAffixes = new ArrayList<>();
        this.explicitAffixes = new ArrayList<>();
        this.tempering = new TemperingData();
        this.masterworking = new MasterworkingData();
        this.socketedGems = new ArrayList<>();
        this.aspectValues = new HashMap<>();
        this.createdAt = System.currentTimeMillis();
    }

    /**
     * 간편 생성자
     */
    public RpgItemData(String templateId, ItemRarity rarity, int itemPower) {
        this();
        this.templateId = templateId;
        this.rarity = rarity;
        this.itemPower = itemPower;
    }

    // ===== 편의 메서드 =====

    /**
     * 모든 어픽스를 반환합니다 (암시적 + 명시적 + 담금질).
     */
    public List<AffixInstance> getAllAffixes() {
        List<AffixInstance> all = new ArrayList<>();
        all.addAll(implicitAffixes);
        all.addAll(explicitAffixes);
        if (tempering != null) {
            if (tempering.getSlot1() != null)
                all.add(tempering.getSlot1());
            if (tempering.getSlot2() != null)
                all.add(tempering.getSlot2());
        }
        return all;
    }

    /**
     * 특정 스탯의 총 합계를 계산합니다.
     */
    public double getTotalStatValue(String statKey) {
        return getAllAffixes().stream()
                .filter(a -> statKey.equals(a.getStatKey()))
                .mapToDouble(AffixInstance::getValue)
                .sum();
    }

    /**
     * Greater Affix 개수를 반환합니다.
     */
    public int getGreaterAffixCount() {
        return (int) explicitAffixes.stream().filter(AffixInstance::isGreater).count();
    }

    /**
     * 전설 등급 이상인지 확인합니다.
     */
    public boolean isLegendaryOrHigher() {
        return rarity.getTier() >= ItemRarity.LEGENDARY.getTier();
    }

    /**
     * 위상이 부여되어 있는지 확인합니다.
     */
    public boolean hasAspect() {
        return aspectId != null && !aspectId.isBlank();
    }

    // ===== Getters & Setters =====

    public String getUuid() {
        return uuid;
    }

    public void setUuid(String uuid) {
        this.uuid = uuid;
    }

    public String getTemplateId() {
        return templateId;
    }

    public void setTemplateId(String templateId) {
        this.templateId = templateId;
    }

    public String getDisplayName() {
        return displayName;
    }

    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }

    public ItemRarity getRarity() {
        return rarity;
    }

    public void setRarity(ItemRarity rarity) {
        this.rarity = rarity;
    }

    public int getItemPower() {
        return itemPower;
    }

    public void setItemPower(int itemPower) {
        this.itemPower = itemPower;
    }

    public int getRequiredLevel() {
        return requiredLevel;
    }

    public void setRequiredLevel(int requiredLevel) {
        this.requiredLevel = requiredLevel;
    }

    public List<AffixInstance> getImplicitAffixes() {
        return implicitAffixes;
    }

    public void setImplicitAffixes(List<AffixInstance> implicitAffixes) {
        this.implicitAffixes = implicitAffixes;
    }

    public List<AffixInstance> getExplicitAffixes() {
        return explicitAffixes;
    }

    public void setExplicitAffixes(List<AffixInstance> explicitAffixes) {
        this.explicitAffixes = explicitAffixes;
    }

    public TemperingData getTempering() {
        return tempering;
    }

    public void setTempering(TemperingData tempering) {
        this.tempering = tempering;
    }

    public MasterworkingData getMasterworking() {
        return masterworking;
    }

    public void setMasterworking(MasterworkingData masterworking) {
        this.masterworking = masterworking;
    }

    public String getAspectId() {
        return aspectId;
    }

    public void setAspectId(String aspectId) {
        this.aspectId = aspectId;
    }

    public double getAspectValue() {
        return aspectValue;
    }

    public void setAspectValue(double aspectValue) {
        this.aspectValue = aspectValue;
    }

    public Map<String, Double> getAspectValues() {
        return aspectValues;
    }

    public void setAspectValues(Map<String, Double> aspectValues) {
        this.aspectValues = aspectValues != null ? aspectValues : new HashMap<>();
    }

    public String getSlotType() {
        return slotType;
    }

    public void setSlotType(String slotType) {
        this.slotType = slotType;
    }

    public String getItemId() {
        return itemId != null ? itemId : uuid;
    }

    public void setItemId(String itemId) {
        this.itemId = itemId;
    }

    // ===== 유니크 스크립트 Getters & Setters =====

    public String getOnEquipScript() {
        return onEquipScript;
    }

    public void setOnEquipScript(String onEquipScript) {
        this.onEquipScript = onEquipScript;
    }

    public String getOnUnequipScript() {
        return onUnequipScript;
    }

    public void setOnUnequipScript(String onUnequipScript) {
        this.onUnequipScript = onUnequipScript;
    }

    public String getOnHitScript() {
        return onHitScript;
    }

    public void setOnHitScript(String onHitScript) {
        this.onHitScript = onHitScript;
    }

    public String getOnTakeDamageScript() {
        return onTakeDamageScript;
    }

    public void setOnTakeDamageScript(String onTakeDamageScript) {
        this.onTakeDamageScript = onTakeDamageScript;
    }

    public String getOnKillScript() {
        return onKillScript;
    }

    public void setOnKillScript(String onKillScript) {
        this.onKillScript = onKillScript;
    }

    public String getUniqueEffectId() {
        return uniqueEffectId;
    }

    public void setUniqueEffectId(String uniqueEffectId) {
        this.uniqueEffectId = uniqueEffectId;
    }

    /**
     * 유니크 스크립트가 설정되어 있는지 확인합니다.
     */
    public boolean hasUniqueScript() {
        return (onEquipScript != null && !onEquipScript.isBlank()) ||
                (onHitScript != null && !onHitScript.isBlank()) ||
                (onTakeDamageScript != null && !onTakeDamageScript.isBlank()) ||
                (onKillScript != null && !onKillScript.isBlank());
    }

    public int getSocketCount() {
        return socketCount;
    }

    public void setSocketCount(int socketCount) {
        this.socketCount = socketCount;
    }

    public List<String> getSocketedGems() {
        return socketedGems;
    }

    public void setSocketedGems(List<String> socketedGems) {
        this.socketedGems = socketedGems;
    }

    public long getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(long createdAt) {
        this.createdAt = createdAt;
    }

    public String getDroppedFrom() {
        return droppedFrom;
    }

    public void setDroppedFrom(String droppedFrom) {
        this.droppedFrom = droppedFrom;
    }
}
