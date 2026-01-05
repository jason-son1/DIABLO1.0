package com.sanctuary.core.data;

import com.sanctuary.core.model.AffixData;
import com.sanctuary.core.model.AspectData;
import com.sanctuary.core.model.ItemBaseData;
import com.sanctuary.core.model.StatData;
import java.util.Collection;

/**
 * 게임 데이터 저장소 인터페이스입니다.
 * 모든 데이터에 대한 SSOT(Single Source of Truth) 역할을 수행합니다.
 */
public interface DataRepository {

    /**
     * 모든 데이터를 다시 로드합니다.
     */
    void reload();

    /**
     * ID로 스탯 데이터를 조회합니다.
     */
    StatData getStat(String id);

    /**
     * 모든 스탯 데이터를 반환합니다.
     */
    Collection<StatData> getAllStats();

    /**
     * 모든 어픽스 데이터를 반환합니다.
     */
    Collection<AffixData> getAllAffixes();

    /**
     * ID로 어픽스 데이터를 조회합니다.
     */
    AffixData getAffix(String id);

    /**
     * ID로 아이템 기본 데이터를 조회합니다.
     */
    ItemBaseData getItemBase(String id);

    /**
     * 모든 아이템 기본 데이터를 반환합니다.
     */
    Collection<ItemBaseData> getAllItemBases();

    /**
     * ID로 위상(Aspect) 데이터를 조회합니다.
     */
    AspectData getAspect(String id);

    /**
     * 모든 위상 데이터를 반환합니다.
     */
    Collection<AspectData> getAllAspects();
}
