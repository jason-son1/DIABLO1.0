package com.sanctuary.core.ecs;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Collection;

/**
 * 엔티티에 부착된 컴포넌트들을 관리하는 컨테이너입니다.
 * 각 컴포넌트 타입은 엔티티당 하나만 존재할 수 있습니다.
 */
public class ComponentContainer {

    private final Map<Class<? extends Component>, Component> components = new HashMap<>();

    /**
     * 컴포넌트를 컨테이너에 부착합니다.
     * 같은 타입의 컴포넌트가 이미 존재하면 교체됩니다.
     * 
     * @param component 부착할 컴포넌트
     * @param <T>       컴포넌트 타입
     * @return 이 컨테이너 (체이닝용)
     */
    public <T extends Component> ComponentContainer attach(T component) {
        if (component == null) {
            throw new IllegalArgumentException("컴포넌트는 null일 수 없습니다.");
        }
        components.put(component.getClass(), component);
        return this;
    }

    /**
     * 지정된 타입의 컴포넌트를 조회합니다.
     * 
     * @param type 조회할 컴포넌트 클래스
     * @param <T>  컴포넌트 타입
     * @return 컴포넌트 또는 null
     */
    @SuppressWarnings("unchecked")
    public <T extends Component> T get(Class<T> type) {
        return (T) components.get(type);
    }

    /**
     * 지정된 타입의 컴포넌트를 Optional로 조회합니다.
     * 
     * @param type 조회할 컴포넌트 클래스
     * @param <T>  컴포넌트 타입
     * @return 컴포넌트를 포함한 Optional
     */
    @SuppressWarnings("unchecked")
    public <T extends Component> Optional<T> getOptional(Class<T> type) {
        return Optional.ofNullable((T) components.get(type));
    }

    /**
     * 지정된 타입의 컴포넌트가 존재하는지 확인합니다.
     * 
     * @param type 확인할 컴포넌트 클래스
     * @return 존재 여부
     */
    public boolean has(Class<? extends Component> type) {
        return components.containsKey(type);
    }

    /**
     * 지정된 타입의 컴포넌트를 제거합니다.
     * 
     * @param type 제거할 컴포넌트 클래스
     * @param <T>  컴포넌트 타입
     * @return 제거된 컴포넌트 또는 null
     */
    @SuppressWarnings("unchecked")
    public <T extends Component> T detach(Class<T> type) {
        return (T) components.remove(type);
    }

    /**
     * 모든 컴포넌트를 반환합니다.
     * 
     * @return 컴포넌트 컬렉션
     */
    public Collection<Component> getAll() {
        return components.values();
    }

    /**
     * 컨테이너를 비웁니다.
     */
    public void clear() {
        components.clear();
    }

    /**
     * 부착된 컴포넌트 개수를 반환합니다.
     * 
     * @return 컴포넌트 개수
     */
    public int size() {
        return components.size();
    }
}
