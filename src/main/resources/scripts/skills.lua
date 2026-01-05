--[[
    skills.lua - 스킬 실행 스크립트
    각 스킬의 효과를 정의합니다.
    함수명: skill_<스킬ID lowercase>
]]

-- ============================================================
-- 야만용사 스킬
-- ============================================================

-- 강타 (BASH) - 기본 스킬
function skill_bash(ctx)
    local damage = ctx.baseDamage
    local player = ctx.player
    
    -- 3회 적중 시 기절 효과
    local bashCount = get_skill_counter(player.uuid, "bash_count") or 0
    bashCount = bashCount + 1
    
    if bashCount >= 3 then
        -- 기절 적용
        apply_stun(ctx.target, 2.0) -- 2초 기절
        set_skill_counter(player.uuid, "bash_count", 0)
        send_message(player.uuid, "§6강타 §e>> §7대상 기절!")
    else
        set_skill_counter(player.uuid, "bash_count", bashCount)
    end
    
    -- 피해 발생
    deal_damage(player.uuid, damage, "PHYSICAL")
    
    return true
end

-- 회오리 (WHIRLWIND) - 핵심 스킬 (채널링)
function skill_whirlwind(ctx)
    local damage = ctx.baseDamage
    local radius = ctx.radius or 4.0
    local player = ctx.player
    
    -- 주변 모든 적에게 피해
    local targets = get_nearby_entities(player.x, player.y, player.z, radius)
    
    for _, target in ipairs(targets) do
        if target.type ~= "PLAYER" then
            deal_damage_to(target.uuid, damage, "PHYSICAL")
        end
    end
    
    return true
end

-- ============================================================
-- 도적 스킬
-- ============================================================

-- 관통 (PUNCTURE) - 기본 스킬
function skill_puncture(ctx)
    local damage = ctx.baseDamage
    local player = ctx.player
    
    -- 3회 적중 시 얼음에 취약
    local punctureCount = get_skill_counter(player.uuid, "puncture_count") or 0
    punctureCount = punctureCount + 1
    
    if punctureCount >= 3 then
        apply_vulnerable(ctx.target, 3.0) -- 3초 취약
        set_skill_counter(player.uuid, "puncture_count", 0)
    else
        set_skill_counter(player.uuid, "puncture_count", punctureCount)
    end
    
    deal_damage(player.uuid, damage, "PHYSICAL")
    
    return true
end

-- 비틀기 칼날 (TWISTING_BLADES) - 핵심 스킬
function skill_twisting_blades(ctx)
    local damage = ctx.baseDamage
    local player = ctx.player
    local rank = ctx.rank or 1
    
    -- 콤보 포인트 보너스 (포인트당 15%)
    local comboPoints = get_combo_points(player.uuid)
    local comboBonus = 1.0 + (comboPoints * 0.15)
    
    -- 1차 피해 (칼날 박기)
    deal_damage(player.uuid, damage * comboBonus, "PHYSICAL")
    
    -- 0.5초 후 2차 피해 (칼날 회수) - 지연 피해
    schedule_damage(player.uuid, ctx.target, damage * 0.5 * comboBonus, 0.5, "PHYSICAL")
    
    -- 콤보 포인트 소모
    consume_combo_points(player.uuid)
    
    return true
end

-- ============================================================
-- 원소술사 스킬
-- ============================================================

-- 불꽃 (SPARK) - 기본 스킬
function skill_spark(ctx)
    local damage = ctx.baseDamage
    local bounceCount = 3
    local player = ctx.player
    
    -- 첫 대상에게 피해
    deal_damage(player.uuid, damage, "LIGHTNING")
    
    -- 튕김 효과 (최대 3회)
    -- TODO: 주변 적에게 튕기는 로직
    
    return true
end

-- 화염구 (FIREBALL) - 핵심 스킬
function skill_fireball(ctx)
    local damage = ctx.baseDamage
    local radius = ctx.radius or 3.0
    local player = ctx.player
    
    -- 주 대상에게 직격 피해
    deal_damage(player.uuid, damage, "FIRE")
    
    -- 주변 적에게 폭발 피해 (50%)
    local nearbyTargets = get_nearby_entities(ctx.target.x, ctx.target.y, ctx.target.z, radius)
    for _, target in ipairs(nearbyTargets) do
        if target.uuid ~= ctx.target.uuid then
            deal_damage_to(target.uuid, damage * 0.5, "FIRE")
        end
    end
    
    -- 마법부여 효과 체크
    if is_enchanted("FIREBALL") then
        -- 적 처치 시 자동 폭발 효과 활성화
        register_on_kill_effect(player.uuid, "fireball_explosion", 5.0)
    end
    
    return true
end

-- ============================================================
-- 강령술사 스킬
-- ============================================================

-- 뼈 파편 (BONE_SPLINTERS) - 기본 스킬
function skill_bone_splinters(ctx)
    local damage = ctx.baseDamage
    local projectiles = 3
    
    -- 3개 파편을 발사
    for i = 1, projectiles do
        deal_damage(ctx.player.uuid, damage, "PHYSICAL")
    end
    
    return true
end

-- 시체 폭발 (CORPSE_EXPLOSION) - 시체 스킬
function skill_corpse_explosion(ctx)
    local damage = ctx.baseDamage
    local radius = ctx.radius or 4.0
    local player = ctx.player
    
    -- 시체 확인
    local corpses = get_nearby_corpses(player.x, player.y, player.z, 10)
    if #corpses == 0 then
        send_message(player.uuid, "§c주변에 시체가 없습니다!")
        return false
    end
    
    -- 첫 번째 시체 폭발
    local corpse = corpses[1]
    consume_corpse(corpse.id)
    
    -- 폭발 피해
    local targets = get_nearby_entities(corpse.x, corpse.y, corpse.z, radius)
    for _, target in ipairs(targets) do
        deal_damage_to(target.uuid, damage, "PHYSICAL")
    end
    
    -- 이펙트
    spawn_particles(corpse.x, corpse.y, corpse.z, "EXPLOSION_HUGE", 10)
    
    return true
end

-- ============================================================
-- 드루이드 스킬
-- ============================================================

-- 폭풍 강타 (STORM_STRIKE) - 기본 스킬
function skill_storm_strike(ctx)
    local damage = ctx.baseDamage
    local chainCount = 2
    
    -- 1차 적중
    deal_damage(ctx.player.uuid, damage, "LIGHTNING")
    
    -- 체인 번개 (최대 2회)
    -- TODO: 주변 적에게 연쇄 피해
    
    -- 25% 확률로 무적 0.5초
    if math.random() < 0.25 then
        apply_immunity(ctx.player.uuid, 0.5)
        send_message(ctx.player.uuid, "§b폭풍 강타 §e>> §7무적 발동!")
    end
    
    return true
end

-- 분쇄 (PULVERIZE) - 핵심 스킬 (곰 변신)
function skill_pulverize(ctx)
    local damage = ctx.baseDamage
    local radius = ctx.radius or 4.0
    local player = ctx.player
    
    -- 곰으로 변신
    set_shapeform(player.uuid, "WEREBEAR", 100)
    
    -- 광역 피해
    local targets = get_nearby_entities(player.x, player.y, player.z, radius)
    for _, target in ipairs(targets) do
        deal_damage_to(target.uuid, damage, "PHYSICAL")
    end
    
    -- 제압 발동 확률 증가
    increase_overpower_chance(player.uuid, 0.20)
    
    return true
end

-- ============================================================
-- 혼령사 스킬
-- ============================================================

-- 천둥 창 (THUNDERSPIKE) - 기본 스킬
function skill_thunderspike(ctx)
    local damage = ctx.baseDamage
    
    deal_damage(ctx.player.uuid, damage, "LIGHTNING")
    
    -- 독수리 영혼: 공격 속도 버프
    if get_spirit_guardian(ctx.player.uuid) == "EAGLE" then
        apply_buff(ctx.player.uuid, "attack_speed", 0.10, 3.0)
    end
    
    return true
end

-- 질주하는 발톱 (RUSHING_CLAW) - 핵심 스킬
function skill_rushing_claw(ctx)
    local damage = ctx.baseDamage
    local hitCount = 3
    local player = ctx.player
    
    -- 3연타 공격
    for i = 1, hitCount do
        deal_damage(player.uuid, damage / hitCount, "FIRE")
    end
    
    -- 재규어 영혼: 연속 피해 보너스
    if get_spirit_guardian(player.uuid) == "JAGUAR" then
        apply_burning(ctx.target, damage * 0.3, 3.0)
    end
    
    return true
end

-- ============================================================
-- 유틸리티 함수 (스텁)
-- ============================================================

-- 이 함수들은 Java에서 LuaBridge를 통해 구현됨
function get_skill_counter(uuid, key)
    return bridge.getSkillCounter(uuid, key)
end

function set_skill_counter(uuid, key, value)
    bridge.setSkillCounter(uuid, key, value)
end

function apply_stun(target, duration)
    bridge.applyStatus(target.uuid, "STUN", duration)
end

function apply_vulnerable(target, duration)
    bridge.applyStatus(target.uuid, "VULNERABLE", duration)
end

function deal_damage(attackerUuid, damage, damageType)
    bridge.dealDamage(attackerUuid, damage, damageType)
end

function deal_damage_to(targetUuid, damage, damageType)
    bridge.dealDamageTo(targetUuid, damage, damageType)
end

function get_nearby_entities(x, y, z, radius)
    return bridge.getNearbyEntities(x, y, z, radius) or {}
end

function schedule_damage(attackerUuid, target, damage, delay, damageType)
    bridge.scheduleDamage(attackerUuid, target.uuid, damage, delay, damageType)
end

function get_combo_points(uuid)
    return bridge.getComboPoints(uuid) or 0
end

function consume_combo_points(uuid)
    bridge.consumeComboPoints(uuid)
end

function is_enchanted(skillId)
    return bridge.isEnchanted(skillId)
end

function register_on_kill_effect(uuid, effectId, duration)
    bridge.registerOnKillEffect(uuid, effectId, duration)
end

function send_message(uuid, message)
    bridge.sendMessage(uuid, message)
end

function get_nearby_corpses(x, y, z, radius)
    return bridge.getNearbyCorpses(x, y, z, radius) or {}
end

function consume_corpse(corpseId)
    bridge.consumeCorpse(corpseId)
end

function spawn_particles(x, y, z, particleType, count)
    bridge.spawnParticles(x, y, z, particleType, count)
end

function set_shapeform(uuid, form, duration)
    bridge.setShapeform(uuid, form, duration)
end

function increase_overpower_chance(uuid, amount)
    bridge.increaseOverpowerChance(uuid, amount)
end

function get_spirit_guardian(uuid)
    return bridge.getSpiritGuardian(uuid)
end

function apply_burning(target, damage, duration)
    bridge.applyBurning(target.uuid, damage, duration)
end

function apply_buff(uuid, buffType, value, duration)
    bridge.applyBuff(uuid, buffType, value, duration)
end

function apply_immunity(uuid, duration)
    bridge.applyImmunity(uuid, duration)
end

print("[Skills.lua] 스킬 스크립트 로드 완료 - 12개 스킬 정의됨")
