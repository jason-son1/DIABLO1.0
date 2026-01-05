package com.sanctuary.core.ecs.component.classmech;

import com.sanctuary.core.ecs.Component;

/**
 * 직업별 고유 메커니즘 인터페이스
 * 각 직업 Component는 이 인터페이스를 구현합니다.
 * 
 * 지원 직업:
 * - BARBARIAN: 무기 기예
 * - ROGUE: 콤보 포인트
 * - SORCERER: 마법부여
 * - NECROMANCER: 정수/시체
 * - DRUID: 영혼 은총
 * - SPIRITBORN: 정령 수호자
 */
public interface ClassMechanic extends Component {

    /**
     * 직업 이름을 반환합니다.
     */
    String getClassName();

    /**
     * 직업 자원 이름을 반환합니다. (예: 분노, 에너지, 마나)
     */
    String getResourceName();

    /**
     * 현재 자원량을 반환합니다.
     */
    double getResource();

    /**
     * 최대 자원량을 반환합니다.
     */
    double getMaxResource();

    /**
     * 자원을 소모합니다.
     * 
     * @return 소모 성공 여부
     */
    boolean consumeResource(double amount);

    /**
     * 자원을 생성합니다.
     */
    void generateResource(double amount);

    /**
     * 매 틱마다 호출됩니다. (자원 재생, 버프 틱다운 등)
     */
    void onTick();

    /**
     * 스킬 사용 시 호출됩니다.
     */
    void onSkillUse(String skillId, String category);

    /**
     * 피해 발생 시 호출됩니다.
     */
    void onDamageDealt(double damage, boolean isCrit, boolean isOverpower);

    /**
     * 피해 받을 때 호출됩니다.
     */
    void onDamageTaken(double damage);

    /**
     * 적 처치 시 호출됩니다.
     */
    void onKill();

    /**
     * 메커니즘을 초기화합니다.
     */
    void reset();

    /**
     * 현재 상태를 설명하는 문자열을 반환합니다.
     */
    String getStatusDisplay();
}
