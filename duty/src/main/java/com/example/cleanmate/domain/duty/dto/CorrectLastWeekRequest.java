package com.example.cleanmate.domain.duty.dto;

import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;

public record CorrectLastWeekRequest(
        @NotNull Long zoneId,
        @NotNull String status,    // "DONE" or "MISSED"
        @NotNull LocalDate weekStart,  // 보정할 주의 월요일 — 반드시 지난 주여야 함
        Long doneById,
        LocalDate doneDate
) {}
