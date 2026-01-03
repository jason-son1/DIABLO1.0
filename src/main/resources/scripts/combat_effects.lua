-- combat_effects.lua
-- 전투 상태 효과 시스템: 취약, 보강, 보호막, 행운의 적중

-- ===================================
-- 취약 (Vulnerable) 시스템
-- ===================================
local VULNERABLE_DURATION = 4000 -- 4초
local VULNERABLE_DAMAGE_BONUS = 0.20 -- 20% 추가 피해

-- 대상에게 취약 상태 적용
function applyVulnerable(target, duration)
    local actualDuration = duration or VULNERABLE_DURATION
    
    sanctuary.addTag(target, "VULNERABLE")
    sanctuary.setTimer(target, "VULNERABLE_EXPIRE", actualDuration, function()
        removeVulnerable(target)
    end)
    
    -- 시각 효과
    sanctuary.playParticle(target, "SPELL_WITCH", 20)
    sanctuary.log("[Vulnerable] 취약 상태 적용: " .. actualDuration .. "ms")
end

-- 취약 상태 제거
function removeVulnerable(target)
    sanctuary.removeTag(target, "VULNERABLE")
    sanctuary.log("[Vulnerable] 취약 상태 종료")
end

-- 취약 상태 확인
function isVulnerable(target)
    return sanctuary.hasTag(target, "VULNERABLE")
end

-- ===================================
-- 보강 (Fortify) 시스템
-- ===================================
local FORTIFY_DECAY_RATE = 2 -- 초당 감소량
local FORTIFY_DR_BONUS = 0.15 -- 보강 상태에서 15% 피해 감소

-- 보강 수치 추가
function addFortify(entity, amount)
    local current = sanctuary.getStat(entity, "FORTIFY") or 0
    local max = sanctuary.getStat(entity, "FORTIFY_MAX") or 500
    
    local newValue = math.min(current + amount, max)
    sanctuary.setStat(entity, "FORTIFY", newValue)
    
    -- 보강 상태 확인 및 태그 갱신
    local hp = sanctuary.getStat(entity, "HP") or 0
    if newValue >= hp then
        if not sanctuary.hasTag(entity, "FORTIFIED") then
            sanctuary.addTag(entity, "FORTIFIED")
            sanctuary.log("[Fortify] 보강 상태 활성화!")
        end
    end
    
    sanctuary.log("[Fortify] 보강 추가: +" .. amount .. " (현재: " .. newValue .. ")")
    
    return newValue
end

-- 보강 수치 감소 (피격 시)
function consumeFortify(entity, damage)
    local current = sanctuary.getStat(entity, "FORTIFY") or 0
    if current <= 0 then return damage end
    
    -- 보강 상태 피해 감소
    if sanctuary.hasTag(entity, "FORTIFIED") then
        damage = damage * (1 - FORTIFY_DR_BONUS)
    end
    
    -- 보강 수치 감소 (피해량의 절반)
    local consumed = math.min(current, damage * 0.5)
    local newValue = current - consumed
    sanctuary.setStat(entity, "FORTIFY", newValue)
    
    -- 보강 상태 해제 확인
    local hp = sanctuary.getStat(entity, "HP") or 0
    if newValue < hp then
        sanctuary.removeTag(entity, "FORTIFIED")
    end
    
    return damage
end

-- 보강 자연 감소 (틱당 호출)
function decayFortify(entity)
    local current = sanctuary.getStat(entity, "FORTIFY") or 0
    if current <= 0 then return end
    
    local newValue = math.max(0, current - FORTIFY_DECAY_RATE)
    sanctuary.setStat(entity, "FORTIFY", newValue)
    
    -- 보강 상태 해제 확인
    local hp = sanctuary.getStat(entity, "HP") or 0
    if newValue < hp then
        sanctuary.removeTag(entity, "FORTIFIED")
    end
end

-- ===================================
-- 보호막 (Barrier) 시스템
-- ===================================
local BARRIER_MAX_DURATION = 10000 -- 10초

-- 보호막 생성
function addBarrier(entity, amount, duration)
    local current = sanctuary.getStat(entity, "BARRIER") or 0
    local newValue = current + amount
    
    sanctuary.setStat(entity, "BARRIER", newValue)
    sanctuary.addTag(entity, "HAS_BARRIER")
    
    -- 시각 효과
    sanctuary.playParticle(entity, "ENCHANT", 30)
    
    -- 지속시간 후 제거
    local actualDuration = duration or BARRIER_MAX_DURATION
    sanctuary.setTimer(entity, "BARRIER_EXPIRE", actualDuration, function()
        expireBarrier(entity, amount)
    end)
    
    sanctuary.log("[Barrier] 보호막 생성: +" .. amount .. " (현재: " .. newValue .. ")")
    
    return newValue
end

-- 보호막 소모 (피격 시)
function consumeBarrier(entity, damage)
    local current = sanctuary.getStat(entity, "BARRIER") or 0
    if current <= 0 then return damage end
    
    -- 보호막이 피해 흡수
    if current >= damage then
        -- 보호막이 모든 피해 흡수
        sanctuary.setStat(entity, "BARRIER", current - damage)
        sanctuary.log("[Barrier] 보호막이 " .. damage .. " 피해 흡수")
        return 0
    else
        -- 보호막 파괴, 남은 피해 전달
        local remainingDamage = damage - current
        sanctuary.setStat(entity, "BARRIER", 0)
        sanctuary.removeTag(entity, "HAS_BARRIER")
        sanctuary.log("[Barrier] 보호막 파괴! 남은 피해: " .. remainingDamage)
        return remainingDamage
    end
end

-- 보호막 만료
function expireBarrier(entity, amount)
    local current = sanctuary.getStat(entity, "BARRIER") or 0
    local newValue = math.max(0, current - amount)
    sanctuary.setStat(entity, "BARRIER", newValue)
    
    if newValue <= 0 then
        sanctuary.removeTag(entity, "HAS_BARRIER")
    end
    
    sanctuary.log("[Barrier] 보호막 만료: -" .. amount)
end

-- ===================================
-- 행운의 적중 (Lucky Hit) 시스템
-- ===================================

-- 행운의 적중 확인
-- 실제 발동 확률 = 스킬 계수 × (1 + 행운의 적중 보너스) × 효과 확률
function checkLuckyHit(caster, skillCoefficient, effectChance)
    local luckyHitBonus = sanctuary.getStat(caster, "LUCKY_HIT") or 0
    local actualChance = skillCoefficient * (1 + luckyHitBonus) * effectChance
    
    local result = math.random() < actualChance
    
    if result then
        sanctuary.log("[Lucky Hit] 행운의 적중 발동! (확률: " .. 
                      string.format("%.1f", actualChance * 100) .. "%)")
    end
    
    return result
end

-- 행운의 적중: 자원 회복
function luckyHitResourceRestore(caster, skillCoefficient, restoreAmount)
    if checkLuckyHit(caster, skillCoefficient, 0.10) then
        local resourceType = sanctuary.getStat(caster, "RESOURCE_TYPE") or "MANA"
        local current = sanctuary.getStat(caster, resourceType) or 0
        local max = sanctuary.getStat(caster, resourceType .. "_MAX") or 100
        
        local newValue = math.min(current + restoreAmount, max)
        sanctuary.setStat(caster, resourceType, newValue)
        
        sanctuary.log("[Lucky Hit] 자원 회복: +" .. restoreAmount)
    end
end

-- 행운의 적중: 생명력 회복
function luckyHitHealRestore(caster, skillCoefficient, healPercent)
    if checkLuckyHit(caster, skillCoefficient, 0.05) then
        local maxHP = sanctuary.getStat(caster, "MAX_HP") or 100
        local healAmount = maxHP * healPercent
        
        local currentHP = sanctuary.getStat(caster, "HP") or 0
        local newHP = math.min(currentHP + healAmount, maxHP)
        sanctuary.setStat(caster, "HP", newHP)
        
        sanctuary.log("[Lucky Hit] 생명력 회복: +" .. math.floor(healAmount))
    end
end

-- ===================================
-- 군중 제어 (Crowd Control) 시스템
-- ===================================

-- 기절 적용
function applyStun(target, duration)
    sanctuary.addTag(target, "STUNNED")
    sanctuary.applyEffect(target, "SLOWNESS", 255, duration)
    sanctuary.applyEffect(target, "JUMP_BOOST", 128, duration)
    
    sanctuary.setTimer(target, "STUN_EXPIRE", duration, function()
        sanctuary.removeTag(target, "STUNNED")
    end)
    
    sanctuary.playParticle(target, "CRIT", 15)
    sanctuary.log("[CC] 기절 적용: " .. duration .. "ms")
end

-- 빙결 적용
function applyFreeze(target, duration)
    sanctuary.addTag(target, "FROZEN")
    sanctuary.applyEffect(target, "SLOWNESS", 255, duration)
    sanctuary.applyEffect(target, "JUMP_BOOST", 128, duration)
    
    sanctuary.setTimer(target, "FREEZE_EXPIRE", duration, function()
        sanctuary.removeTag(target, "FROZEN")
    end)
    
    sanctuary.playParticle(target, "SNOW_SHOVEL", 30)
    sanctuary.log("[CC] 빙결 적용: " .. duration .. "ms")
end

-- 감속 적용
function applySlow(target, level, duration)
    sanctuary.addTag(target, "SLOWED")
    sanctuary.applyEffect(target, "SLOWNESS", level, duration)
    
    sanctuary.setTimer(target, "SLOW_EXPIRE", duration, function()
        sanctuary.removeTag(target, "SLOWED")
    end)
    
    sanctuary.log("[CC] 감속 적용: 레벨 " .. level .. ", " .. duration .. "ms")
end

-- ===================================
-- 피격 처리 (메인 함수)
-- ===================================
function onDamageReceived(target, damage, damageType, attacker)
    local finalDamage = damage
    
    -- 1. 보호막 소모
    if sanctuary.hasTag(target, "HAS_BARRIER") then
        finalDamage = consumeBarrier(target, finalDamage)
        if finalDamage <= 0 then
            return 0
        end
    end
    
    -- 2. 보강 효과 적용
    if sanctuary.getStat(target, "FORTIFY") > 0 then
        finalDamage = consumeFortify(target, finalDamage)
    end
    
    -- 3. 방어력/저항 감소는 damage_calculator에서 처리
    
    return finalDamage
end

sanctuary.log("combat_effects.lua 로드 완료!")
