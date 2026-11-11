package com.example.cleanmate.domain.history.dto;

import com.example.cleanmate.domain.duty.model.DutyStatus;

import java.time.LocalDate;
import java.util.List;

public record HistoryResponse(List<WeekEntry> weeks) {
    public record WeekEntry(LocalDate weekStart, List<DutyItem> duties) {}
    public record DutyItem(
        Long zoneId, String zoneName,
        Long assigneeId, String assigneeName,
        DutyStatus status,
        Long doneById, String doneByName, LocalDate doneDate
    ) {}
}
