package com.example.cleanmate.global.event;

/**
 * 완료 이벤트 발행 인터페이스.
 * 클라우드 단계에서 RedisStreamsEventPublisher로 교체.
 */
@FunctionalInterface
public interface EventPublisher {
    void publish(DutyEvent event);
}
