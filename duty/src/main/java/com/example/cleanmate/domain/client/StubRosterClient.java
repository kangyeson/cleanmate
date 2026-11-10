package com.example.cleanmate.domain.client;

import java.time.LocalDate;
import java.util.List;

/**
 * 단위 테스트 전용 스텁. Spring 빈으로 등록하지 않는다.
 * 프로덕션에서는 HttpRosterClient가 RosterClient 역할을 맡는다.
 */
public class StubRosterClient implements RosterClient {

    @Override
    public RosterResponse getRoster() {
        return new RosterResponse(
                1L,
                "2026-2학기 525호",
                LocalDate.of(2026, 11, 2),  // 방 시작 주 (월요일)
                List.of(
                        new RosterResponse.MemberInfo(1L, "민정"),
                        new RosterResponse.MemberInfo(2L, "지현"),
                        new RosterResponse.MemberInfo(3L, "서현"),
                        new RosterResponse.MemberInfo(4L, "예손")
                ),
                List.of(
                        new RosterResponse.ZoneInfo(1L, "샤워실"),
                        new RosterResponse.ZoneInfo(2L, "화장실"),
                        new RosterResponse.ZoneInfo(3L, "세면대"),
                        new RosterResponse.ZoneInfo(4L, "바닥")
                )
        );
    }
}
