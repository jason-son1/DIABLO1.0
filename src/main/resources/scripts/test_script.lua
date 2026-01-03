-- test_script.lua
-- Sanctuary Lua 연동 테스트 스크립트

-- 기본 테스트 함수
function helloWorld()
    sanctuary.log("Hello from Lua! Sanctuary 스크립트 엔진이 정상 작동합니다.")
    return true
end

-- 스탯 조회 테스트
function testStats(entity)
    local strength = sanctuary.getStat(entity, "STRENGTH")
    sanctuary.log("힘 스탯: " .. tostring(strength))
    return strength
end

-- 태그 확인 테스트
function testTags(entity)
    if sanctuary.hasTag(entity, "ELITE") then
        sanctuary.log("정예 몬스터입니다!")
        return true
    else
        sanctuary.log("일반 몬스터입니다.")
        return false
    end
end

-- 스킬 발동 예시
function onSkillCast(caster, target, skillId)
    sanctuary.log("스킬 발동: " .. skillId)
    
    -- 간단한 데미지 계산 예시
    local baseDamage = 100
    local strength = sanctuary.getStat(caster, "STRENGTH")
    local finalDamage = baseDamage * (1 + strength * 0.1)
    
    sanctuary.log("최종 데미지: " .. tostring(finalDamage))
    return finalDamage
end

-- 피격 시 효과 예시
function onHit(attacker, victim)
    -- 취약 상태 확인
    if sanctuary.hasTag(victim, "VULNERABLE") then
        sanctuary.log("대상이 취약 상태입니다! 추가 피해 적용")
        return 1.2 -- 20% 추가 피해
    end
    return 1.0
end

-- 초기화 로그
sanctuary.log("test_script.lua 로드 완료!")
