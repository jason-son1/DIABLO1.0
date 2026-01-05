package com.sanctuary.world.event;

import com.sanctuary.DiabloPlugin;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;

/**
 * 월드 이벤트 관리자
 * 지옥물결, 레기온 이벤트 등 서버 전체 이벤트를 관리합니다.
 */
public class WorldEventManager {

    private final Logger logger;
    private final DiabloPlugin plugin;

    // 활성 이벤트
    private final Map<String, WorldEvent> activeEvents = new ConcurrentHashMap<>();

    // 이벤트 핸들러
    private final Map<String, WorldEventHandler> handlers = new HashMap<>();

    // 이벤트 틱 태스크
    private BukkitRunnable tickTask;

    public WorldEventManager(Logger logger, DiabloPlugin plugin) {
        this.logger = logger;
        this.plugin = plugin;
    }

    /**
     * 이벤트 핸들러를 등록합니다.
     */
    public void registerHandler(String eventType, WorldEventHandler handler) {
        handlers.put(eventType, handler);
        logger.info("[WorldEventManager] 핸들러 등록: " + eventType);
    }

    /**
     * 이벤트를 시작합니다.
     */
    public boolean startEvent(String eventId, String eventType, Map<String, Object> params) {
        if (activeEvents.containsKey(eventId)) {
            return false;
        }

        WorldEventHandler handler = handlers.get(eventType);
        if (handler == null) {
            logger.warning("[WorldEventManager] 핸들러 없음: " + eventType);
            return false;
        }

        WorldEvent event = new WorldEvent(eventId, eventType, params);
        event.start();
        activeEvents.put(eventId, event);

        handler.onStart(event);
        logger.info("[WorldEventManager] 이벤트 시작: " + eventId);
        return true;
    }

    /**
     * 이벤트를 종료합니다.
     */
    public void endEvent(String eventId) {
        WorldEvent event = activeEvents.remove(eventId);
        if (event != null) {
            event.end();

            WorldEventHandler handler = handlers.get(event.getEventType());
            if (handler != null) {
                handler.onEnd(event);
            }

            logger.info("[WorldEventManager] 이벤트 종료: " + eventId);
        }
    }

    /**
     * 이벤트를 조회합니다.
     */
    public WorldEvent getEvent(String eventId) {
        return activeEvents.get(eventId);
    }

    /**
     * 특정 타입의 활성 이벤트를 조회합니다.
     */
    public List<WorldEvent> getActiveEventsByType(String eventType) {
        List<WorldEvent> result = new ArrayList<>();
        for (WorldEvent event : activeEvents.values()) {
            if (event.getEventType().equals(eventType)) {
                result.add(event);
            }
        }
        return result;
    }

    /**
     * 틱 태스크를 시작합니다.
     */
    public void startTickTask() {
        tickTask = new BukkitRunnable() {
            @Override
            public void run() {
                for (WorldEvent event : new ArrayList<>(activeEvents.values())) {
                    WorldEventHandler handler = handlers.get(event.getEventType());
                    if (handler != null) {
                        handler.onTick(event);
                    }

                    // 시간 초과 확인
                    if (event.getDurationSeconds() > 0 && event.getElapsedSeconds() >= event.getDurationSeconds()) {
                        endEvent(event.getEventId());
                    }
                }
            }
        };
        tickTask.runTaskTimer(plugin, 20L, 20L); // 매 초
    }

    /**
     * 종료합니다.
     */
    public void shutdown() {
        if (tickTask != null) {
            tickTask.cancel();
        }

        for (String eventId : new ArrayList<>(activeEvents.keySet())) {
            endEvent(eventId);
        }
    }

    /**
     * 이벤트 핸들러 인터페이스
     */
    public interface WorldEventHandler {
        void onStart(WorldEvent event);

        void onTick(WorldEvent event);

        void onEnd(WorldEvent event);
    }

    /**
     * 월드 이벤트 데이터
     */
    public static class WorldEvent {
        private final String eventId;
        private final String eventType;
        private final Map<String, Object> params;
        private long startTime;
        private long endTime;
        private int durationSeconds;

        public WorldEvent(String eventId, String eventType, Map<String, Object> params) {
            this.eventId = eventId;
            this.eventType = eventType;
            this.params = params != null ? params : new HashMap<>();
        }

        public void start() {
            this.startTime = System.currentTimeMillis();
        }

        public void end() {
            this.endTime = System.currentTimeMillis();
        }

        public long getElapsedSeconds() {
            long end = endTime > 0 ? endTime : System.currentTimeMillis();
            return (end - startTime) / 1000;
        }

        public String getEventId() {
            return eventId;
        }

        public String getEventType() {
            return eventType;
        }

        public Map<String, Object> getParams() {
            return params;
        }

        @SuppressWarnings("unchecked")
        public <T> T getParam(String key, T defaultValue) {
            Object value = params.get(key);
            return value != null ? (T) value : defaultValue;
        }

        public int getDurationSeconds() {
            return durationSeconds;
        }

        public void setDurationSeconds(int durationSeconds) {
            this.durationSeconds = durationSeconds;
        }
    }
}
