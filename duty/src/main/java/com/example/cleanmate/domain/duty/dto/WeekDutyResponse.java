package com.example.cleanmate.domain.duty.dto;

import com.example.cleanmate.domain.duty.model.DutyStatus;

import java.time.LocalDate;
import java.util.List;

/**
 * 이번 주 현황 응답.
 *
 * doneCount·total은 v2에서 추가 (클라우드 배포 시험의 새 버전).
 * 기존 필드는 그대로 두고 더하기만 했으므로, 카나리 중 v1·v2가 섞여 응답해도
 * 화면(index.js, duties로 직접 셈)과 알림 서버(reminder.js)는 그대로 동작한다.
 */
public record WeekDutyResponse(
        LocalDate weekStart,
        List<DutyItem> duties,
        int doneCount,      // v2: 이번 주 완료한 구역 수
        int total           // v2: 이번 주 구역 수
) {
    public record DutyItem(
            Long zoneId,
            String zoneName,
            Long assigneeId,
            String assigneeName,
            DutyStatus status,
            Long doneById,
            String doneByName,  // null이면 미완료. 완료면 처리한 사람 이름(본인=본인 이름, 대신 청소=대신한 사람 이름)
            LocalDate doneDate
    ) {}
}
