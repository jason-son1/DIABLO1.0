package com.sanctuary.combat.calc;

import com.sanctuary.combat.model.DamageContext;
import com.sanctuary.combat.stat.Stat;
import com.sanctuary.combat.stat.AttributeContainer;

import java.util.concurrent.ThreadLocalRandom;

/**
 * DamageCalculator (데미지 계산기)
 * 디아블로 4 시즌 6 공식 적용
 *
 * 공식: FinalDamage = WeaponDamage * (1 + AdditiveTotal) * Multipliers * Crit *
 * Vuln
 * + Overpower Damage (if proc)
 */
public class DamageCalculator {

    public double calculate(DamageContext ctx) {
        AttributeContainer stats = ctx.getAttackerStats();

        // 1. 무기 데미지 (Weapon Damage)
        // 실제로는 Min-Max 사이의 랜덤값이여야 함. 여기서는 Base 값을 Random 범위로 가정하거나 단순화.
        // TODO: Items 모듈에서 무기 Min/Max 가져오기 구현 필요. 현재는 WEAPON_DAMAGE 스탯 하나로 처리.
        double weaponDamageBase = stats.getValue(Stat.WEAPON_DAMAGE);

        // 스킬 계수 적용 (예: 화염구 150%)
        double baseDamage = weaponDamageBase * ctx.getSkillCoefficient();

        // 2. 주 스탯 보너스 (Main Stat Bonus) - 합연산 버킷과 별도로 적용되는 경우가 많음, D4에서는 스킬별 스케일링 태그에
        // 따름
        // 여기서는 단순화를 위해 모든 직업이 힘(Strength) 기반이라고 가정하고 0.1% 계수 적용
        double mainStat = stats.getValue(Stat.STRENGTH);
        double mainStatMultiplier = 1.0 + (mainStat * 0.001); // 1000 STR -> +100% (x2.0)

        // 3. 합연산 버킷 (Additive Bucket)
        // 근거리, 원거리, 물리, 속성 피해 등등을 모두 합산
        double additiveBonus = 0.0;

        // 속성 피해 (Physical, Fire, etc)
        if (ctx.hasTag("PHYSICAL"))
            additiveBonus += stats.getValue(Stat.PHYSICAL_DAMAGE);
        if (ctx.hasTag("FIRE"))
            additiveBonus += stats.getValue(Stat.FIRE_DAMAGE);
        // ... 기타 속성 ...

        // 조건부 피해 (Close, Distant)
        // 실제로는 거리 계산 필요
        // (ctx.getAttacker().getLocation().distance(ctx.getVictim().getLocation()))
        // 여기서는 값을 가져온다고 가정
        additiveBonus += stats.getValue(Stat.DAMAGE_VS_CLOSE); // 예시

        // 시즌 6 변경점: 치명타 피해와 취약 피해의 '초과분'은 합연산 버킷에 들어감
        // 하지만 여기서는 간단하게 합연산 레이어에 이미 포함되어 있다고 가정하거나,
        // 별도 로직을 짤 수 있음.
        // 현재 AttributeContainer 구조상 user가 addAdditive(CRIT_DAMAGE, 0.2) 했다면 여기에 포함됨.
        additiveBonus += stats.getValue(Stat.CRIT_DAMAGE); // 합연산용 크저피
        additiveBonus += stats.getValue(Stat.VULNERABLE_DAMAGE); // 합연산용 취약피해

        double additiveMultiplier = 1.0 + additiveBonus;

        // 4. 곱연산 그룹 (Global Multipliers)
        // 위상(Aspect)이나 패시브에서 [x]로 붙는 것들
        double globalMultiplier = stats.getValue(Stat.GLOBAL_DAMAGE_MULTI); // 기본 1.0, 증가 시 곱해져서 저장되어 있음

        // 5. 치명타 및 취약 (Crit & Vuln)
        // 결정 로직
        double critChance = stats.getValue(Stat.CRIT_CHANCE);
        boolean isCrit = ThreadLocalRandom.current().nextDouble() < critChance;
        ctx.setCritical(isCrit);

        // 취약 여부 판단 (상태이상매니저를 통해 확인해야 함, 여기서는 Context에 미리 세팅되었다고 가정)
        boolean isVuln = ctx.isVulnerable();

        // 고정 배율 적용 (시즌 6 Hard Cap)
        double critMultiplier = isCrit ? 1.5 : 1.0;
        double vulnMultiplier = isVuln ? 1.2 : 1.0;

        // --- 중간 계산 (Intermediate) ---
        double currentDamage = baseDamage * mainStatMultiplier * additiveMultiplier * globalMultiplier * critMultiplier
                * vulnMultiplier;

        // 6. 제압 (Overpower)
        // 3% 고정 확률
        boolean isOverpower = ThreadLocalRandom.current().nextDouble() < 0.03;
        if (isOverpower) {
            ctx.setOverpower(true);
            double maxHp = stats.getValue(Stat.MAX_HP);
            double fortify = 0.0; // TODO: Fortify 구현 후 연동
            double overpowerBonus = stats.getValue(Stat.OVERPOWER_DAMAGE); // 합연산 보너스

            // 공식: (CurrentHP + Fortify) * Multiplier
            // 여기서는 단순화: (MaxHP + Fortify) * 1.5 * (1 + Bonus)
            double overpowerDamage = (maxHp + fortify) * 1.5 * (1.0 + overpowerBonus);

            // 제압 피해는 기존 데미지에 더해짐 (Gained as additive damage scaling based on life?)
            // D4 정확한 공식: it adds to the damage.
            currentDamage += overpowerDamage;
        }

        return currentDamage;
    }
}
