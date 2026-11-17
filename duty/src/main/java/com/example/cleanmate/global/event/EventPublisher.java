package com.example.cleanmate.global.event;

/**
 * 완료 이벤트 발행 인터페이스.
 * 운영: RedisStreamEventPublisher (Redis Streams)
 */
@FunctionalInterface
public interface EventPublisher {
    void publish(DutyEvent event);
}
