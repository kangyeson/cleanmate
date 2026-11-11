package com.example.cleanmate.domain.stats.dto;

import java.util.List;

public record StatsResponse(List<MemberStat> members) {

    public record MemberStat(
            Long memberId,
            String name,
            int assigned,
            int selfDone,
            int missed,
            int substitute,
            String rate   // null이면 JSON에서 null
    ) {}
}
