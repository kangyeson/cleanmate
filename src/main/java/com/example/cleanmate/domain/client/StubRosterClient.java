package com.example.cleanmate.domain.client;

import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.List;

/**
 * 개발·테스트용 스텁. 인수인계 문서 3장 예시 데이터를 반환한다.
 * 인증 서버(HttpRosterClient) 완성 후 이 클래스를 제거하고 교체한다.
 */
@Component
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
                        new RosterResponse.ZoneInfo(10L, "샤워실"),
                        new RosterResponse.ZoneInfo(20L, "화장실"),
                        new RosterResponse.ZoneInfo(30L, "세면대"),
                        new RosterResponse.ZoneInfo(40L, "바닥")
                )
        );
    }
}
