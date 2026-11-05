package com.example.cleanmate.global.event;

/**
 * 완료·대신 청소 이벤트.
 * 현재는 no-op 발행. 클라우드 단계에서 Redis Streams 발행으로 교체.
 */
public record DutyEvent(
        Long roomId,
        Long zoneId,
        Long assigneeId,  // 담당자
        Long doneById,    // 실제 청소한 사람 (대신 청소이면 담당자와 다름)
        boolean isSubstitute
) {
}
