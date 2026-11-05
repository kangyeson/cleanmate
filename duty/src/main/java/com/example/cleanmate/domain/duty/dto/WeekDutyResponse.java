package com.example.cleanmate.domain.duty.dto;

import com.example.cleanmate.domain.duty.model.DutyStatus;

import java.time.LocalDate;
import java.util.List;

public record WeekDutyResponse(
        LocalDate weekStart,
        List<DutyItem> duties
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
