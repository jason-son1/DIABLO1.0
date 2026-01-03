package com.sanctuary.core.model;

/**
 * 스탯 데이터를 담는 DTO 클래스입니다.
 * 외부 JSON 파일로부터 로드됩니다.
 */
public class StatData {
    private String id;
    private String name;
    private String type; // 예: CORE, OFFENSIVE, DEFENSIVE, UTILITY

    public StatData() {
    }

    public StatData(String id, String name, String type) {
        this.id = id;
        this.name = name;
        this.type = type;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    @Override
    public String toString() {
        return "StatData{id='" + id + "', name='" + name + "', type='" + type + "'}";
    }
}
