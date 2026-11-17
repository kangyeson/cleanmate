package com.example.cleanmate.global.event;

import java.time.LocalDate;

/**
 * 완료·대신 청소 이벤트 (Redis Streams "duty-events"로 발행).
 *
 * 이름과 이번 주 완료 수도 함께 담아 알림 서버(Node.js)가 알림 문구를 만들 수 있게 한다.
 */
public record DutyEvent(
        Long roomId,
        LocalDate weekStart,
        Long zoneId,
        String zoneName,
        Long assigneeId,      // 담당자
        String assigneeName,
        Long doneById,        // 실제 청소한 사람 (대신 청소이면 담당자와 다름)
        String doneByName,
        boolean isSubstitute,
        int doneCount,        // 이 기록 뒤 이번 주 완료된 구역 수
        int total             // 이번 주 전체 구역 수
) {
}
