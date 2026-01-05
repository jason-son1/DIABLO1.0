--[[
    loot_rules.lua - 전리품 규칙 오버라이드 스크립트
    
    스마트 룻 시스템을 커스터마이징합니다.
    이 스크립트의 함수들은 SmartLootManager에서 호출됩니다.
]]

local sanctuary = require("sanctuary")

-- =========================================================
-- 스마트 룻 오버라이드
-- 플레이어별 커스텀 드롭 확률을 설정합니다.
-- =========================================================

-- 기본 직업별 가중치
local CLASS_WEIGHTS = {
    BARBARIAN = {
        MELEE_WEAPON = 1.5,
        HEAVY_ARMOR = 1.4,
        STRENGTH_GEAR = 1.3
    },
    SORCERER = {
        STAFF = 1.5,
        FOCUS = 1.4,
        INTELLIGENCE_GEAR = 1.3
    },
    ROGUE = {
        DAGGER = 1.5,
        BOW = 1.4,
        DEXTERITY_GEAR = 1.3
    },
    NECROMANCER = {
        SCYTHE = 1.5,
        FOCUS = 1.4,
        WILLPOWER_GEAR = 1.3
    },
    DRUID = {
        STAFF = 1.5,
        TOTEM = 1.4,
        WILLPOWER_GEAR = 1.3
    },
    SPIRITBORN = {
        GLAIVE = 1.5,
        QUARTERSTAFF = 1.4,
        ALL_STATS_GEAR = 1.3
    }
}

-- 특별 드롭 이벤트 (월드 보스, 지옥 물결 등)
local SPECIAL_EVENTS = {
    world_boss = {
        legendary_chance = 0.25,
        mythic_chance = 0.02
    },
    helltide = {
        legendary_chance = 0.15,
        unique_chance = 0.05
    },
    nightmare_dungeon = {
        legendary_chance = 0.20,
        greater_affix_chance = 0.10
    }
}

--[[
    스마트 룻 오버라이드 함수
    @param context 플레이어 정보 테이블
    @return nil (기본 로직 사용) 또는 가중치 테이블
]]
function smart_loot_override(context)
    local playerClass = context.playerClass
    local playerLevel = context.playerLevel
    
    -- 레벨 50 이상: 고급 아이템 비중 증가
    if playerLevel >= 50 then
        -- TODO: 고급 아이템 가중치 조정
        return nil -- 현재는 기본 로직 사용
    end
    
    -- 특정 직업 커스텀 로직
    if playerClass == "SPIRITBORN" then
        -- 혼령사 전용 아이템 가중치 부스트
        -- 시즌 6 특별 처리
        return nil
    end
    
    return nil -- 기본 로직 사용
end

--[[
    아이템 파워 오버라이드
    @param context 플레이어 정보 테이블
    @return nil (기본 로직) 또는 아이템 파워 숫자
]]
function item_power_override(context)
    local playerLevel = context.playerLevel
    local worldTier = context.worldTier or 1
    
    -- 기본 공식: 100 + (레벨 * 15) * 월드티어 배수
    local basePower = 100 + (playerLevel * 15)
    local tierMultiplier = ({1.0, 1.25, 1.5, 2.0})[worldTier] or 1.0
    
    return math.floor(basePower * tierMultiplier)
end

--[[
    희귀도 결정 오버라이드
    @param context 플레이어/몬스터 정보
    @return nil 또는 희귀도 문자열 (COMMON, MAGIC, RARE, LEGENDARY, UNIQUE, MYTHIC)
]]
function rarity_override(context)
    local monsterType = context.monsterType
    local playerLuck = context.playerLuck or 0
    
    -- 월드 보스 처치 시 전설 보장
    if monsterType == "WORLD_BOSS" then
        local roll = math.random()
        if roll < 0.02 then
            return "MYTHIC"
        elseif roll < 0.10 then
            return "UNIQUE"
        else
            return "LEGENDARY"
        end
    end
    
    -- 엘리트 몬스터: 전설 확률 증가
    if monsterType == "ELITE" then
        local roll = math.random()
        local legendaryChance = 0.05 + (playerLuck * 0.001)
        if roll < legendaryChance then
            return "LEGENDARY"
        elseif roll < legendaryChance + 0.15 then
            return "RARE"
        end
    end
    
    return nil -- 기본 로직 사용
end

--[[
    그레이터 어픽스 확률 오버라이드
    높은 월드 티어와 악몽 던전 레벨에서 증가
]]
function greater_affix_chance_override(context)
    local worldTier = context.worldTier or 1
    local dungeonLevel = context.dungeonLevel or 0
    
    -- 월드 티어 4 기본 3%, 악몽 던전 레벨당 0.1% 추가
    if worldTier >= 4 then
        return 0.03 + (dungeonLevel * 0.001)
    end
    
    return 0 -- 월드 티어 4 미만은 그레이터 어픽스 없음
end

print("[LootRules] 전리품 규칙 스크립트 로드 완료")
