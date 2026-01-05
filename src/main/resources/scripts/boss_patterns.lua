--[[
    boss_patterns.lua - 보스 패턴 정의
    
    각 보스는 페이즈별 행동 패턴을 가집니다.
    HP 임계값에 따라 페이즈가 전환되며, 각 페이즈에서 사용하는 스킬이 정의됩니다.
]]

-- 보스 패턴 저장소
local boss_patterns = {}

-- ============================================================
-- 해골왕 (Skeleton King)
-- ============================================================
boss_patterns["skeleton_king"] = {
    name = "해골왕",
    baseLevel = 50,
    
    -- 페이즈 정의
    phases = {
        -- 페이즈 1: HP 100% ~ 70%
        {
            phase = 1,
            hp_threshold = 1.0,
            hp_end = 0.7,
            skills = {
                { id = "ground_slam", cooldown = 10, weight = 30 },
                { id = "bone_throw", cooldown = 5, weight = 50 },
                { id = "summon_skeletons", cooldown = 20, weight = 20 }
            },
            on_enter = function(boss)
                sanctuary.log("[Boss] 해골왕 페이즈 1 시작")
                sanctuary.bossSay(boss, "누가 나의 잠을 깨웠느냐!")
            end
        },
        
        -- 페이즈 2: HP 70% ~ 30%
        {
            phase = 2,
            hp_threshold = 0.7,
            hp_end = 0.3,
            skills = {
                { id = "whirlwind", cooldown = 8, weight = 35 },
                { id = "bone_prison", cooldown = 15, weight = 25 },
                { id = "summon_elite_skeleton", cooldown = 25, weight = 20 },
                { id = "death_coil", cooldown = 6, weight = 20 }
            },
            on_enter = function(boss)
                sanctuary.log("[Boss] 해골왕 페이즈 2 시작")
                sanctuary.bossSay(boss, "나의 진정한 힘을 보여주마!")
                -- 페이즈 전환 버프
                sanctuary.applyBuff(boss, "enrage", 0.2, 999)
            end
        },
        
        -- 페이즈 3: HP 30% ~ 0% (광폭화)
        {
            phase = 3,
            hp_threshold = 0.3,
            hp_end = 0,
            enrage = true,
            skills = {
                { id = "death_spiral", cooldown = 5, weight = 40 },
                { id = "mass_grave", cooldown = 15, weight = 30 },
                { id = "bone_storm", cooldown = 10, weight = 30 }
            },
            on_enter = function(boss)
                sanctuary.log("[Boss] 해골왕 페이즈 3 (광폭화)")
                sanctuary.bossSay(boss, "모두 함께 죽으리라!!!")
                -- 광폭화 버프
                sanctuary.applyBuff(boss, "enrage", 0.5, 999)
                sanctuary.applyBuff(boss, "attack_speed", 0.3, 999)
            end
        }
    },
    
    -- 스킬 정의
    skills = {
        ground_slam = {
            name = "대지 강타",
            damage = 2.0, -- 피해 배율
            radius = 5,
            mechanic = "GROUND_AOE",
            delay = 1.5, -- 발동 전 표시 시간
            on_use = function(boss, targets)
                sanctuary.playEffect(boss, "GROUND_CRACK", 5)
                return true
            end
        },
        
        bone_throw = {
            name = "뼈 투척",
            damage = 1.2,
            mechanic = "PROJECTILE",
            on_use = function(boss, target)
                sanctuary.shootProjectile(boss, target, "BONE", 1.5)
                return true
            end
        },
        
        summon_skeletons = {
            name = "해골 소환",
            summonCount = 4,
            summonId = "skeleton_minion",
            on_use = function(boss)
                for i = 1, 4 do
                    local offset = { x = math.random(-3, 3), y = 0, z = math.random(-3, 3) }
                    sanctuary.summonMob("skeleton_minion", boss, offset)
                end
                sanctuary.playEffect(boss, "SUMMONING_CIRCLE", 3)
                return true
            end
        },
        
        death_spiral = {
            name = "죽음의 소용돌이",
            damage = 3.0,
            radius = 8,
            mechanic = "ROTATING_BEAM",
            duration = 5,
            on_use = function(boss)
                sanctuary.playEffect(boss, "DEATH_SPIRAL", 8)
                return true
            end
        }
    }
}

-- ============================================================
-- 타락한 감독관 (Fallen Overseer)
-- ============================================================
boss_patterns["fallen_overseer"] = {
    name = "타락한 감독관",
    baseLevel = 30,
    
    phases = {
        {
            phase = 1,
            hp_threshold = 1.0,
            hp_end = 0.5,
            skills = {
                { id = "whip_strike", cooldown = 4, weight = 50 },
                { id = "call_reinforcements", cooldown = 20, weight = 30 },
                { id = "terrify", cooldown = 15, weight = 20 }
            }
        },
        {
            phase = 2,
            hp_threshold = 0.5,
            hp_end = 0,
            enrage = true,
            skills = {
                { id = "frenzy_whip", cooldown = 3, weight = 40 },
                { id = "blood_sacrifice", cooldown = 25, weight = 30 },
                { id = "demonic_roar", cooldown = 10, weight = 30 }
            }
        }
    }
}

-- ============================================================
-- 지옥 군주 (Pit Lord)
-- ============================================================
boss_patterns["pit_lord"] = {
    name = "지옥 군주",
    baseLevel = 70,
    
    phases = {
        {
            phase = 1,
            hp_threshold = 1.0,
            hp_end = 0.8,
            skills = {
                { id = "infernal_slash", cooldown = 6, weight = 40 },
                { id = "hellfire_breath", cooldown = 12, weight = 35 },
                { id = "demon_summon", cooldown = 25, weight = 25 }
            }
        },
        {
            phase = 2,
            hp_threshold = 0.8,
            hp_end = 0.5,
            skills = {
                { id = "meteor_strike", cooldown = 15, weight = 30 },
                { id = "infernal_charge", cooldown = 10, weight = 35 },
                { id = "hellfire_breath", cooldown = 10, weight = 35 }
            }
        },
        {
            phase = 3,
            hp_threshold = 0.5,
            hp_end = 0.2,
            skills = {
                { id = "rain_of_fire", cooldown = 8, weight = 35 },
                { id = "demonic_portal", cooldown = 20, weight = 30 },
                { id = "infernal_slash", cooldown = 4, weight = 35 }
            }
        },
        {
            phase = 4,
            hp_threshold = 0.2,
            hp_end = 0,
            enrage = true,
            skills = {
                { id = "apocalypse", cooldown = 30, weight = 20 },
                { id = "rain_of_fire", cooldown = 5, weight = 40 },
                { id = "infernal_charge", cooldown = 6, weight = 40 }
            }
        }
    }
}

-- ============================================================
-- 보스 AI 함수
-- ============================================================

-- 현재 페이즈 확인
function getBossPhase(bossId, hpPercent)
    local pattern = boss_patterns[bossId]
    if not pattern then return nil end
    
    for _, phase in ipairs(pattern.phases) do
        if hpPercent <= phase.hp_threshold and hpPercent > phase.hp_end then
            return phase
        end
    end
    
    return pattern.phases[#pattern.phases]
end

-- 스킬 선택 (가중치 기반)
function selectBossSkill(phase)
    if not phase or not phase.skills then return nil end
    
    local totalWeight = 0
    for _, skill in ipairs(phase.skills) do
        totalWeight = totalWeight + skill.weight
    end
    
    local roll = math.random(1, totalWeight)
    local current = 0
    
    for _, skill in ipairs(phase.skills) do
        current = current + skill.weight
        if roll <= current then
            return skill.id
        end
    end
    
    return phase.skills[1].id
end

-- 페이즈 전환 체크
function checkPhaseTransition(bossId, previousHp, currentHp)
    local pattern = boss_patterns[bossId]
    if not pattern then return false end
    
    local prevPhase = getBossPhase(bossId, previousHp / 100)
    local currPhase = getBossPhase(bossId, currentHp / 100)
    
    if prevPhase and currPhase and prevPhase.phase ~= currPhase.phase then
        sanctuary.log("[Boss] 페이즈 전환: " .. prevPhase.phase .. " -> " .. currPhase.phase)
        
        if currPhase.on_enter then
            currPhase.on_enter(bossId)
        end
        
        return true, currPhase.phase
    end
    
    return false, nil
end

-- 보스 패턴 조회
function getBossPattern(bossId)
    return boss_patterns[bossId]
end

print("[boss_patterns.lua] 보스 패턴 정의 로드 완료 - " .. 3 .. "개 보스")
