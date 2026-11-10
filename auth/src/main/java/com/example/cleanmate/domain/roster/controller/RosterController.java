package com.example.cleanmate.domain.roster.controller;

import com.example.cleanmate.domain.member.repository.MemberRepository;
import com.example.cleanmate.domain.roster.dto.RosterResponse;
import com.example.cleanmate.global.apiPayload.ApiResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;

/**
 * duty 서버 전용 내부 엔드포인트.
 * 방·룸메이트·구역 정보를 한 번에 반환.
 *
 * TODO: 3단계에서 rooms·zones 테이블 조회로 교체.
 */
@RestController
public class RosterController {

    // 구역 목록 — 3단계에서 zones 테이블로 교체
    private static final List<RosterResponse.ZoneInfo> ZONES = List.of(
            new RosterResponse.ZoneInfo(1L, "샤워실"),
            new RosterResponse.ZoneInfo(2L, "화장실"),
            new RosterResponse.ZoneInfo(3L, "세면대"),
            new RosterResponse.ZoneInfo(4L, "바닥")
    );

    private final MemberRepository memberRepository;

    public RosterController(MemberRepository memberRepository) {
        this.memberRepository = memberRepository;
    }

    @GetMapping("/internal/roster")
    public ApiResponse<RosterResponse> getRoster() {
        List<RosterResponse.MemberInfo> members = memberRepository
                .findAllByRoomIdOrderByTurnOrder(1L).stream()
                .map(m -> new RosterResponse.MemberInfo(m.getId(), m.getName()))
                .toList();

        // TODO: 3단계에서 rooms 테이블 조회로 교체 (roomId, roomName, startWeek)
        RosterResponse roster = new RosterResponse(
                1L, "2026-2학기 525호", LocalDate.of(2026, 11, 2),
                members, ZONES
        );
        return ApiResponse.ok(roster);
    }
}
