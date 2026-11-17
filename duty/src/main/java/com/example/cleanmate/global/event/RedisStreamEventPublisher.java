package com.example.cleanmate.global.event;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.connection.stream.RecordId;
import org.springframework.data.redis.connection.stream.StreamRecords;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 완료 이벤트를 Redis Streams(duty-events)에 넣는 EventPublisher.
 *
 * - 발행 시점: DB 트랜잭션이 커밋된 뒤(afterCommit). 커밋 전에 넣으면 롤백된 기록의
 *   소식이 나갈 수 있기 때문이다. 트랜잭션 밖에서 불리면 바로 넣는다.
 * - 실패 처리: 발행이 실패해도 예외를 던지지 않는다. 완료 기록은 이미 커밋됐고
 *   기록이 소식보다 중요하므로, 소식만 빠지고 오류 로그를 남긴다(클라우드 기획설계 4.4).
 * - 소비: 알림 서버(Node.js)가 소비자 그룹 notify로 읽고 처리가 끝나면 XACK한다.
 *   알림 서버가 꺼져 있어도 이벤트는 스트림에 남아 있다가 다시 켜지면 처리된다.
 */
@Component
public class RedisStreamEventPublisher implements EventPublisher {

    private static final Logger log = LoggerFactory.getLogger(RedisStreamEventPublisher.class);

    /** 스트림 이름 (알림 서버와 같은 값) */
    public static final String STREAM_KEY = "duty-events";

    /** 스트림에 남길 최대 이벤트 수 (대략값). Redis 메모리가 계속 늘지 않게 한다. */
    private static final long MAX_LEN = 1000;

    private final StringRedisTemplate redis;
    private final String instanceId;

    public RedisStreamEventPublisher(StringRedisTemplate redis,
                                     @Value("${INSTANCE_ID:local}") String instanceId) {
        this.redis = redis;
        this.instanceId = instanceId;
    }

    @Override
    public void publish(DutyEvent event) {
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            // 지금 트랜잭션이 커밋된 뒤에 보내도록 예약
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    send(event);
                }
            });
        } else {
            send(event);
        }
    }

    /** XADD duty-events * type DONE ... 후 길이를 MAXLEN ~1000으로 줄인다. */
    private void send(DutyEvent e) {
        try {
            // 스트림 값은 문자열만 가능하므로 모든 필드를 문자열로 넣는다 (순서 유지)
            Map<String, String> fields = new LinkedHashMap<>();
            fields.put("type", "DONE");
            fields.put("roomId", String.valueOf(e.roomId()));
            fields.put("weekStart", String.valueOf(e.weekStart()));
            fields.put("zoneId", String.valueOf(e.zoneId()));
            fields.put("zoneName", e.zoneName());
            fields.put("assigneeId", String.valueOf(e.assigneeId()));
            fields.put("assigneeName", e.assigneeName());
            fields.put("doneById", String.valueOf(e.doneById()));
            fields.put("doneByName", e.doneByName());
            fields.put("substitute", String.valueOf(e.isSubstitute()));
            fields.put("doneCount", String.valueOf(e.doneCount()));
            fields.put("total", String.valueOf(e.total()));
            fields.put("instance", instanceId);   // 어느 당번 서버가 발행했는지

            RecordId id = redis.opsForStream().add(StreamRecords.string(fields).withStreamKey(STREAM_KEY));
            redis.opsForStream().trim(STREAM_KEY, MAX_LEN, true);
            log.info("완료 이벤트 발행 {} ({} {} {}/{})", id, e.zoneName(), e.doneByName(), e.doneCount(), e.total());
        } catch (Exception ex) {
            log.error("완료 이벤트 발행 실패 — 기록은 저장됨, 소식만 빠짐: {}", ex.getMessage());
        }
    }
}
