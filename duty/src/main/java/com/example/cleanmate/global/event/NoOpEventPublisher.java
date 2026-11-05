package com.example.cleanmate.global.event;

import org.springframework.stereotype.Component;

/**
 * 아무것도 하지 않는 EventPublisher.
 * 클라우드 단계에서 Redis Streams 구현으로 교체.
 */
@Component
public class NoOpEventPublisher implements EventPublisher {

    @Override
    public void publish(DutyEvent event) {
        // no-op: 클라우드 단계에서 Redis Streams 발행으로 교체
    }
}
