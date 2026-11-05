package com.example.cleanmate.domain.client;

import java.time.LocalDate;
import java.util.List;

/**
 * 인증 서버 GET /internal/roster 응답.
 * 현재 활성 방의 룸메이트·구역 정보를 담는다.
 */
public record RosterResponse(
        Long roomId,
        String roomName,
        LocalDate startWeek,
        List<MemberInfo> members,   // turn_order 오름차순
        List<ZoneInfo> zones        // zone_order 오름차순
) {
    public record MemberInfo(Long id, String name) {}
    public record ZoneInfo(Long id, String name) {}

    public List<Long> memberIds() {
        return members.stream().map(MemberInfo::id).toList();
    }

    public List<Long> zoneIds() {
        return zones.stream().map(ZoneInfo::id).toList();
    }

    public String memberName(Long id) {
        return members.stream()
                .filter(m -> m.id().equals(id))
                .map(MemberInfo::name)
                .findFirst()
                .orElse("알 수 없음");
    }

    public String zoneName(Long id) {
        return zones.stream()
                .filter(z -> z.id().equals(id))
                .map(ZoneInfo::name)
                .findFirst()
                .orElse("알 수 없음");
    }
}
