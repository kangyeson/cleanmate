package com.example.cleanmate.domain.client;

/** duty 서버 내부 API 클라이언트. */
public interface DutyClient {
    /** 방 종료 처리: 빠진 주 배정 채우기 + 이번 주 포함 모든 PENDING → MISSED. */
    void finalizeRoom(Long roomId);

    /**
     * 이번 주 배정 보장: 설정 변경 전 호출.
     * duty 서버 POST /internal/rooms/{roomId}/current-week
     */
    void ensureCurrentWeek(Long roomId);
}
