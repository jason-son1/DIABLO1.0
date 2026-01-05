--------------------------------------------------------------------------------
-- combat_effects.lua
-- 전투 효과 및 이벤트 훅 예제 스크립트
-- 
-- 사용 가능한 sanctuary API:
-- 로깅: log(msg), warn(msg)
-- 스탯/태그: getStat(entity, key), hasTag(entity, tag), addTag(entity, tag), removeTag(entity, tag)
-- 체력: getHealth(entity), setHealth(entity, hp), getMaxHealth(entity), heal(entity, amount), damage(entity, amount)
-- 보강: getFortify(entity), setFortify(entity, amount), addFortify(entity, amount), isFortified(entity)
-- 보호막: getBarrier(entity), setBarrier(entity, amount), addBarrier(entity, amount), hasBarrier(entity)
-- 이펙트: playSound(entity, soundName), spawnParticle(entity, particleName, count)
--------------------------------------------------------------------------------

-- 전역 설정
local FORTIFY_PER_HIT = 5.0        -- 적 처치 시 보강 획득량
local BARRIER_DURATION_BONUS = 1.5 -- 보호막 보너스 배율

--------------------------------------------------------------------------------
-- 전투 이벤트 훅 함수들
-- CombatEventBus.registerLuaHook() 으로 등록됨
--------------------------------------------------------------------------------

-- 치명타 발생 시 호출
function onCriticalHit(event)
    local ctx = event.context
    if not ctx then return end
    
    local attacker = { uuid = ctx.attackerId }
    local target = { uuid = ctx.victimId }
    
    -- 치명타 시 파티클 효과
    sanctuary.spawnParticle(target, "CRIT", 15)
    sanctuary.playSound(attacker, "ENTITY_PLAYER_ATTACK_CRIT")
    
    sanctuary.log("치명타 발생! 피해: " .. event.finalDamage)
end

-- 제압(Overpower) 발생 시 호출
function onOverpower(event)
    local ctx = event.context
    if not ctx then return end
    
    local target = { uuid = ctx.victimId }
    
    -- 제압 시 큰 파티클 효과
    sanctuary.spawnParticle(target, "EXPLOSION_LARGE", 1)
    sanctuary.playSound(target, "ENTITY_GENERIC_EXPLODE")
    
    sanctuary.log("제압 발동! 추가 피해: " .. event.overpowerDamage)
end

-- 적 처치 시 호출
function onKill(event)
    local ctx = event.context
    if not ctx then return end
    
    local attacker = { uuid = ctx.attackerId }
    
    -- 처치 시 보강 획득
    local currentFortify = sanctuary.getFortify(attacker)
    sanctuary.addFortify(attacker, FORTIFY_PER_HIT)
    
    sanctuary.log("적 처치! 보강: " .. currentFortify .. " -> " .. sanctuary.getFortify(attacker))
end

-- 피해 적용 직전 호출 (피해량 수정 가능)
function onBeforeDamage(context)
    -- 예: 보호막이 있으면 피해 10% 감소
    local target = { uuid = context.victimId }
    
    if sanctuary.hasBarrier(target) then
        return { damageMultiplier = 0.9 }
    end
    
    return nil
end

-- 피해 적용 직후 호출
function onAfterDamage(context, finalDamage)
    -- 예: 피해 로그
    sanctuary.log("피해 적용 완료: " .. finalDamage)
end

--------------------------------------------------------------------------------
-- 유틸리티 함수
--------------------------------------------------------------------------------

-- 엔티티 체력 비율 확인
function getHealthPercent(entity)
    local current = sanctuary.getHealth(entity)
    local max = sanctuary.getMaxHealth(entity)
    if max == 0 then return 0 end
    return (current / max) * 100
end

-- 보강 활성화 상태 확인 (D4: 보강 >= 현재 체력)
function checkFortifyActive(entity)
    return sanctuary.isFortified(entity)
end

-- 긴급 보호막 활성화 (체력 20% 이하 시)
function emergencyBarrier(entity)
    local hpPercent = getHealthPercent(entity)
    if hpPercent <= 20 and not sanctuary.hasBarrier(entity) then
        local maxHp = sanctuary.getMaxHealth(entity)
        sanctuary.addBarrier(entity, maxHp * 0.3)  -- 최대 체력의 30% 보호막
        sanctuary.playSound(entity, "BLOCK_ENCHANTMENT_TABLE_USE")
        sanctuary.spawnParticle(entity, "END_ROD", 30)
        return true
    end
    return false
end

--------------------------------------------------------------------------------
-- 스킬 효과 예제
--------------------------------------------------------------------------------

-- 화염구 효과
function fireballEffect(caster, target, skillData)
    -- 화염 파티클
    sanctuary.spawnParticle(target, "FLAME", 20)
    sanctuary.playSound(target, "ITEM_FIRECHARGE_USE")
    
    -- 취약 태그 추가 (3초 후 자동 제거는 Java에서 처리)
    sanctuary.addTag(target, "VULNERABLE")
    
    -- 보호막 제거
    if sanctuary.hasBarrier(target) then
        local barrier = sanctuary.getBarrier(target)
        sanctuary.setBarrier(target, barrier * 0.5)  -- 보호막 50% 감소
    end
end

-- 회오리 바람 효과 (야만용사)
function whirlwindEffect(caster, targets, skillData)
    -- 시전자 파티클
    sanctuary.spawnParticle(caster, "SWEEP_ATTACK", 10)
    
    -- 각 타겟에 효과 적용
    for i, target in ipairs(targets) do
        sanctuary.spawnParticle(target, "CRIT", 5)
    end
end

-- 보호막 스킬 (원소술사)
function barrierSkill(caster, skillData)
    local maxHp = sanctuary.getMaxHealth(caster)
    local barrierAmount = maxHp * 0.4 * BARRIER_DURATION_BONUS
    
    sanctuary.addBarrier(caster, barrierAmount)
    sanctuary.spawnParticle(caster, "END_ROD", 50)
    sanctuary.playSound(caster, "BLOCK_BEACON_ACTIVATE")
    
    return barrierAmount
end

sanctuary.log("[combat_effects.lua] 로드 완료")
