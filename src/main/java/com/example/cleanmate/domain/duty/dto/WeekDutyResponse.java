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
            String doneByName,  // null: 미완료 또는 본인 완료
            LocalDate doneDate
    ) {}
}
