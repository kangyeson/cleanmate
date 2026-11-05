package com.example.cleanmate.domain.duty.model;

public enum DutyStatus {
    PENDING,  // 배정됨, 아직 처리 전
    DONE,     // 완료 (본인 또는 대신)
    MISSED    // 미완료 확정 (WeeklyJob 이 주 전환 시 확정)
}
