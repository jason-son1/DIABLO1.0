--[[
    unique_items.lua - 유니크 아이템 효과 스크립트
    
    각 유니크 아이템은 고유한 효과를 가지며, 이 스크립트에서 정의됩니다.
    효과 함수는 context 객체를 받으며, sanctuary API를 통해 게임과 상호작용합니다.
    
    사용 가능한 context 필드:
    - player: 플레이어 정보
    - target: 대상 정보 (피해 이벤트 시)
    - damage: 피해량 (피해 이벤트 시)
    - itemData: 아이템 데이터
]]

local sanctuary = require("sanctuary")

-- =========================================================
-- [유니크 무기] 도살자의 칼날 (Butcher's Cleaver)
-- 효과: 피해 시 10% 확률로 대상을 출혈시킴
-- =========================================================
function butchers_cleaver_on_hit(context)
    local roll = math.random()
    if roll < 0.10 then
        -- 출혈 적용 (5초간 총 피해의 20%)
        local bleedDamage = context.damage * 0.20
        sanctuary.applyDot(context.target, "bleed", bleedDamage, 5)
        sanctuary.spawnParticle(context.target, "REDSTONE", 30)
        sanctuary.playSound(context.target, "ENTITY_PLAYER_HURT", 1.0, 0.5)
        return true
    end
    return false
end

-- =========================================================
-- [유니크 갑옷] 그랜드파더의 유산 (Grandfather's Legacy)
-- 효과: 장착 시 최대 체력 25% 증가
-- =========================================================
function grandfathers_legacy_on_equip(context)
    local player = context.player
    local currentMax = sanctuary.getMaxHealth(player)
    local bonus = currentMax * 0.25
    
    -- 보너스 체력 저장 (해제 시 복구용)
    sanctuary.setPlayerData(player, "grandfather_bonus", bonus)
    sanctuary.modifyMaxHealth(player, bonus)
    sanctuary.healPlayer(player, bonus) -- 추가된 체력만큼 회복
    
    sanctuary.sendMessage(player, "§6[유니크] §f조상의 축복이 당신을 보호합니다!")
    return true
end

function grandfathers_legacy_on_unequip(context)
    local player = context.player
    local bonus = sanctuary.getPlayerData(player, "grandfather_bonus") or 0
    
    sanctuary.modifyMaxHealth(player, -bonus)
    sanctuary.setPlayerData(player, "grandfather_bonus", nil)
    
    return true
end

-- =========================================================
-- [유니크 반지] 운명의 원환 (Ring of Fate)
-- 효과: 적 처치 시 5% 확률로 전리품 2배
-- =========================================================
function ring_of_fate_on_kill(context)
    local roll = math.random()
    if roll < 0.05 then
        -- 행운 플래그 설정 (드롭 시스템에서 체크)
        context.doubleLoot = true
        sanctuary.spawnParticle(context.target, "TOTEM", 50)
        sanctuary.playSound(context.player, "ENTITY_PLAYER_LEVELUP", 1.0, 1.5)
        sanctuary.sendMessage(context.player, "§e§l운명이 미소짓습니다! §f전리품 2배!")
        return true
    end
    return false
end

-- =========================================================
-- [유니크 부적] 하르로가스의 심장 (Heart of Harrogath)
-- 효과: 피격 시 보강이 없으면 체력의 10%만큼 보강 생성
-- =========================================================
function heart_of_harrogath_on_take_damage(context)
    local player = context.player
    local currentFortify = sanctuary.getFortify(player)
    
    if currentFortify <= 0 then
        local maxHealth = sanctuary.getMaxHealth(player)
        local fortifyAmount = maxHealth * 0.10
        
        sanctuary.addFortify(player, fortifyAmount)
        sanctuary.spawnParticle(player, "ENCHANTMENT_TABLE", 30)
        sanctuary.playSound(player, "BLOCK_BEACON_ACTIVATE", 0.5, 1.2)
        return true
    end
    return false
end

-- =========================================================
-- [신화 무기] 릴리스의 눈물 (Tears of Lilith)
-- 효과: 피해 시 대상 최대 체력의 1% 추가 피해 (보스에게 최대 50)
-- =========================================================
function tears_of_lilith_on_hit(context)
    local target = context.target
    local targetMaxHealth = sanctuary.getMaxHealth(target)
    local bonusDamage = targetMaxHealth * 0.01
    
    -- 보스 캡 적용
    if sanctuary.hasTag(target, "boss") then
        bonusDamage = math.min(bonusDamage, 50)
    end
    
    -- 추가 피해 적용
    sanctuary.dealDamage(target, bonusDamage, "mythic")
    sanctuary.spawnParticle(target, "SOUL_FIRE_FLAME", 15)
    
    return true
end

-- =========================================================
-- 유니크 효과 레지스트리
-- 아이템 데이터의 스크립트 필드와 연결
-- =========================================================
UNIQUE_EFFECTS = {
    -- 무기
    butchers_cleaver = {
        onHit = butchers_cleaver_on_hit
    },
    
    -- 갑옷
    grandfathers_legacy = {
        onEquip = grandfathers_legacy_on_equip,
        onUnequip = grandfathers_legacy_on_unequip
    },
    
    -- 반지
    ring_of_fate = {
        onKill = ring_of_fate_on_kill
    },
    
    -- 부적
    heart_of_harrogath = {
        onTakeDamage = heart_of_harrogath_on_take_damage
    },
    
    -- 신화
    tears_of_lilith = {
        onHit = tears_of_lilith_on_hit
    }
}

-- 효과 실행 헬퍼 함수
function executeUniqueEffect(effectId, eventType, context)
    local effect = UNIQUE_EFFECTS[effectId]
    if effect and effect[eventType] then
        return effect[eventType](context)
    end
    return false
end

print("[UniqueItems] 유니크 아이템 효과 로드 완료 - " .. 
      #(table.keys(UNIQUE_EFFECTS) or {}) .. "개 등록")
