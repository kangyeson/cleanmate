package com.example.cleanmate.domain.roster.dto;

import java.time.LocalDate;
import java.util.List;

/**
 * /internal/roster 응답 DTO.
 * duty 서버의 RosterResponse와 JSON 필드명을 동일하게 맞춤.
 */
public record RosterResponse(
        Long roomId,
        String roomName,
        LocalDate startWeek,
        List<MemberInfo> members,
        List<ZoneInfo> zones
) {
    public record MemberInfo(Long id, String name) {}
    public record ZoneInfo(Long id, String name) {}
}
