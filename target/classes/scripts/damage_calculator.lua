-- damage_calculator.lua
-- 디아블로 4 데미지 버킷 시스템 구현
-- 최종 데미지 = 무기 × 주스탯 × 합연산 × 곱연산 × 치명타 × 취약

-- 상수 정의
local CRIT_BASE_MULTIPLIER = 1.5      -- 치명타 기본 배율
local VULNERABLE_BASE_MULTIPLIER = 1.2 -- 취약 기본 배율
local OVERPOWER_BASE_CHANCE = 0.03    -- 제압 기본 확률
local ARMOR_CAP = 9230                -- 방어력 캡 (85% 감소)
local MAX_PHYSICAL_DR = 0.85          -- 최대 물리 피해 감소
local RESISTANCE_SOFT_CAP = 0.70      -- 저항 소프트 캡
local RESISTANCE_HARD_CAP = 0.85      -- 저항 하드 캡

-- 주 스탯 배율 계산
function calculateMainStatMultiplier(mainStatValue)
    -- 주 스탯 1당 0.1% 증폭 (독립적 버킷)
    return 1 + (mainStatValue * 0.001)
end

-- 합연산 버킷 계산 (Additive Bucket)
-- 근거리 피해, 원거리 피해, 군중제어 피해, 출혈 피해, 
-- 추가 치명타 피해, 추가 취약 피해 등
function calculateAdditiveBucket(stats, target)
    local total = 0
    
    -- 근거리/원거리 조건부 피해
    local distance = stats.distanceToTarget or 0
    if distance <= 5 then
        total = total + (stats.DAMAGE_TO_CLOSE or 0)
    else
        total = total + (stats.DAMAGE_TO_DISTANT or 0)
    end
    
    -- 군중 제어 대상 피해
    if target and hasAnyTag(target, {"STUNNED", "FROZEN", "SLOWED"}) then
        total = total + (stats.DAMAGE_TO_CC or 0)
    end
    
    -- 출혈/지속 피해
    if target and sanctuary.hasTag(target, "BLEEDING") then
        total = total + (stats.BLEED_DAMAGE or 0)
    end
    
    -- 추가 치명타/취약 피해는 여기에 합산 (시즌 6 변경사항)
    -- 기본 배율 초과분만 추가
    total = total + (stats.CRIT_DAMAGE or 0)
    total = total + (stats.VULNERABLE_DAMAGE or 0)
    
    return 1 + total
end

-- 곱연산 버킷 계산 (Multiplicative Bucket)
-- 전설 위상, 핵심 패시브, 특수 효과 등
function calculateMultiplicativeBucket(aspects)
    local result = 1
    
    if aspects then
        for _, aspect in ipairs(aspects) do
            if aspect.multiplier then
                result = result * aspect.multiplier
            end
        end
    end
    
    return result
end

-- 치명타 배율 계산
function calculateCritMultiplier(isCrit, extraCritDamage)
    if not isCrit then
        return 1
    end
    -- 기본 1.5배 (추가분은 합연산 버킷에서 처리)
    return CRIT_BASE_MULTIPLIER
end

-- 취약 배율 계산
function calculateVulnerableMultiplier(target)
    if target and sanctuary.hasTag(target, "VULNERABLE") then
        -- 기본 1.2배 (추가분은 합연산 버킷에서 처리)
        return VULNERABLE_BASE_MULTIPLIER
    end
    return 1
end

-- 제압 피해 계산
function calculateOverpowerDamage(baseDamage, caster, overpowerBonus)
    local currentHP = sanctuary.getStat(caster, "HP") or 0
    local fortify = sanctuary.getStat(caster, "FORTIFY") or 0
    
    local overpowerBase = currentHP + fortify
    local multiplier = 1 + (overpowerBonus or 0)
    
    return overpowerBase * multiplier * 0.03
end

-- 제압 발동 여부 확인
function checkOverpower()
    return math.random() < OVERPOWER_BASE_CHANCE
end

-- 방어력 기반 물리 피해 감소 계산
function calculatePhysicalDR(armor)
    local dr = armor / ARMOR_CAP
    return math.min(dr, MAX_PHYSICAL_DR)
end

-- 저항 기반 원소 피해 감소 계산
function calculateElementalDR(resistance, hasCapBreaker)
    local cap = hasCapBreaker and RESISTANCE_HARD_CAP or RESISTANCE_SOFT_CAP
    return math.min(resistance, cap)
end

-- 최종 데미지 계산 (메인 함수)
function calculateFinalDamage(caster, target, skillData)
    -- 1. 무기 기본 피해
    local weaponDamage = sanctuary.getStat(caster, "WEAPON_DAMAGE") or 100
    
    -- 스킬 계수 적용
    local skillCoefficient = skillData.baseDamage or 1.0
    local baseDamage = weaponDamage * skillCoefficient
    
    -- 2. 주 스탯 배율
    local mainStatKey = sanctuary.getStat(caster, "MAIN_STAT_TYPE") or "STRENGTH"
    local mainStatValue = sanctuary.getStat(caster, mainStatKey) or 0
    local mainStatMultiplier = calculateMainStatMultiplier(mainStatValue)
    
    -- 3. 스탯 수집
    local stats = {
        distanceToTarget = skillData.distanceToTarget or 5,
        DAMAGE_TO_CLOSE = sanctuary.getStat(caster, "DAMAGE_TO_CLOSE") or 0,
        DAMAGE_TO_DISTANT = sanctuary.getStat(caster, "DAMAGE_TO_DISTANT") or 0,
        DAMAGE_TO_CC = sanctuary.getStat(caster, "DAMAGE_TO_CC") or 0,
        BLEED_DAMAGE = sanctuary.getStat(caster, "BLEED_DAMAGE") or 0,
        CRIT_DAMAGE = sanctuary.getStat(caster, "CRIT_DAMAGE") or 0,
        VULNERABLE_DAMAGE = sanctuary.getStat(caster, "VULNERABLE_DAMAGE") or 0,
    }
    
    -- 4. 합연산 버킷
    local additiveBucket = calculateAdditiveBucket(stats, target)
    
    -- 5. 곱연산 버킷 (위상 효과 등)
    local aspects = {} -- 플레이어 위상 목록
    local multiplicativeBucket = calculateMultiplicativeBucket(aspects)
    
    -- 6. 치명타 확인
    local critChance = sanctuary.getStat(caster, "CRIT_CHANCE") or 0.05
    local isCrit = math.random() < critChance
    local critMultiplier = calculateCritMultiplier(isCrit, stats.CRIT_DAMAGE)
    
    -- 7. 취약 배율
    local vulnMultiplier = calculateVulnerableMultiplier(target)
    
    -- 8. 최종 계산
    local finalDamage = baseDamage * mainStatMultiplier * additiveBucket * 
                        multiplicativeBucket * critMultiplier * vulnMultiplier
    
    -- 9. 제압 피해 추가 (확률 발동)
    if checkOverpower() then
        local overpowerBonus = sanctuary.getStat(caster, "OVERPOWER_DAMAGE") or 0
        local overpowerDamage = calculateOverpowerDamage(finalDamage, caster, overpowerBonus)
        finalDamage = finalDamage + overpowerDamage
        sanctuary.log("[Overpower] 제압 발동! +" .. math.floor(overpowerDamage) .. " 피해")
    end
    
    -- 로그 출력
    local critStr = isCrit and " [치명타]" or ""
    local vulnStr = vulnMultiplier > 1 and " [취약]" or ""
    sanctuary.log("[Damage] 최종 피해: " .. math.floor(finalDamage) .. critStr .. vulnStr)
    
    return {
        damage = finalDamage,
        isCrit = isCrit,
        isVulnerable = vulnMultiplier > 1,
        isOverpower = false
    }
end

-- 피해 감소 계산 (피격 시)
function calculateDamageReduction(target, incomingDamage, damageType)
    local armor = sanctuary.getStat(target, "ARMOR") or 0
    local reduction = 0
    
    if damageType == "PHYSICAL" then
        reduction = calculatePhysicalDR(armor)
    else
        local resistKey = "RESISTANCE_" .. damageType
        local resistance = sanctuary.getStat(target, resistKey) or 0
        reduction = calculateElementalDR(resistance, false)
    end
    
    -- 보강 효과
    local fortify = sanctuary.getStat(target, "FORTIFY") or 0
    local currentHP = sanctuary.getStat(target, "HP") or 0
    if fortify >= currentHP then
        reduction = reduction + 0.15 -- 보강 상태: 추가 15% 감소
    end
    
    -- 피해 감소 스탯
    local damageReduction = sanctuary.getStat(target, "DAMAGE_REDUCTION") or 0
    reduction = reduction + damageReduction
    
    -- 최대 감소율 제한 (95%)
    reduction = math.min(reduction, 0.95)
    
    local finalDamage = incomingDamage * (1 - reduction)
    
    sanctuary.log("[Defense] 피해 감소: " .. math.floor(reduction * 100) .. "% -> " .. 
                  math.floor(finalDamage) .. " 피해")
    
    return finalDamage
end

-- 헬퍼: 태그 확인
function hasAnyTag(entity, tags)
    for _, tag in ipairs(tags) do
        if sanctuary.hasTag(entity, tag) then
            return true
        end
    end
    return false
end

sanctuary.log("damage_calculator.lua 로드 완료!")
