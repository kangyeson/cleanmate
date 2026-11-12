package com.example.cleanmate.domain.stats.service;

import com.example.cleanmate.domain.client.RosterClient;
import com.example.cleanmate.domain.client.RosterResponse;
import com.example.cleanmate.domain.duty.model.Duty;
import com.example.cleanmate.domain.duty.model.WeekCalendar;
import com.example.cleanmate.domain.duty.repository.InMemoryDutyRepository;
import com.example.cleanmate.domain.stats.dto.StatsResponse;
import com.example.cleanmate.domain.stats.dto.StatsResponse.MemberStat;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class StatsServiceTest {

    // 멤버 ID: 민정=1, 지현=2, 서현=3, 예손=4, roomId=1
    private static final Long ROOM_ID   = 1L;
    private static final Long MINJEONG  = 1L;
    private static final Long JIHYUN    = 2L;
    private static final Long SEOHYUN   = 3L;
    private static final Long YESON     = 4L;

    // 고정 날짜: 2026-11-10 (화요일) → currentMonday = 2026-11-09
    private static final ZoneId SEOUL = ZoneId.of("Asia/Seoul");
    private static final Clock FIXED_CLOCK = Clock.fixed(
            Instant.parse("2026-11-10T00:00:00Z"), SEOUL);

    // 지난 주(확정된 주): 2026-11-02 (월요일, < 2026-11-09)
    private static final LocalDate LAST_WEEK    = LocalDate.of(2026, 11, 2);
    // 이번 주(미확정): 2026-11-09
    private static final LocalDate CURRENT_WEEK = LocalDate.of(2026, 11, 9);

    private static final Long ZONE_1 = 1L;
    private static final Long ZONE_2 = 2L;
    private static final Long ZONE_3 = 3L;
    private static final Long ZONE_4 = 4L;

    private StatsService buildService(InMemoryDutyRepository repo) {
        RosterClient stubRoster = new com.example.cleanmate.domain.client.StubRosterClient();
        WeekCalendar calendar = new WeekCalendar(FIXED_CLOCK);
        return new StatsService(repo, stubRoster, calendar);
    }

    /**
     * 시나리오 (확정된 주 = LAST_WEEK 만 집계):
     * - 지난 주 민정: DONE(본인) → assigned+1, selfDone+1
     * - 지난 주 지현: PENDING → assigned+1, missed+1
     * - 지난 주 예손이 서현 대신: DONE(doneById=예손, assigneeId=서현)
     *   → 서현: assigned+1, missed=0, selfDone=0
     *   → 예손: substitute+1
     * - 이번 주 민정: PENDING → 확정된 주가 아니므로 집계에서 제외
     */
    @Test
    void getStats_본인완료_미완료_대신청소_포함() {
        InMemoryDutyRepository repo = new InMemoryDutyRepository();

        // 지난 주 민정 DONE(본인)
        Duty minjeongDone = repo.save(Duty.newAssignment(ROOM_ID, LAST_WEEK, ZONE_1, MINJEONG));
        minjeongDone.markDone(MINJEONG, LAST_WEEK.plusDays(2));
        repo.save(minjeongDone);

        // 지난 주 지현 PENDING(지난주=미완료)
        repo.save(Duty.newAssignment(ROOM_ID, LAST_WEEK, ZONE_2, JIHYUN));

        // 지난 주 서현 담당 duty를 예손이 대신
        Duty seohyunDuty = repo.save(Duty.newAssignment(ROOM_ID, LAST_WEEK, ZONE_3, SEOHYUN));
        seohyunDuty.markDone(YESON, LAST_WEEK.plusDays(3));
        repo.save(seohyunDuty);

        // 이번 주 민정 PENDING
        repo.save(Duty.newAssignment(ROOM_ID, CURRENT_WEEK, ZONE_1, MINJEONG));

        StatsService service = buildService(repo);
        StatsResponse response = service.getStats();
        List<MemberStat> members = response.members();

        MemberStat minjeong = members.stream().filter(m -> m.memberId().equals(MINJEONG)).findFirst().orElseThrow();
        MemberStat jihyun   = members.stream().filter(m -> m.memberId().equals(JIHYUN)).findFirst().orElseThrow();
        MemberStat seohyun  = members.stream().filter(m -> m.memberId().equals(SEOHYUN)).findFirst().orElseThrow();
        MemberStat yeson    = members.stream().filter(m -> m.memberId().equals(YESON)).findFirst().orElseThrow();

        // 민정: assigned=1(지난주만), selfDone=1, missed=0, substitute=0, rate="100%"
        assertEquals(1, minjeong.assigned());
        assertEquals(1, minjeong.selfDone());
        assertEquals(0, minjeong.missed());
        assertEquals(0, minjeong.substitute());
        assertEquals("100%", minjeong.rate());

        // 지현: assigned=1, selfDone=0, missed=1(PENDING+지난주), substitute=0, rate="0%"
        assertEquals(1, jihyun.assigned());
        assertEquals(0, jihyun.selfDone());
        assertEquals(1, jihyun.missed());
        assertEquals(0, jihyun.substitute());
        assertEquals("0%", jihyun.rate());

        // 서현: assigned=1, selfDone=0, missed=0(DONE이니까), substitute=0, rate="0%"
        assertEquals(1, seohyun.assigned());
        assertEquals(0, seohyun.selfDone());
        assertEquals(0, seohyun.missed());
        assertEquals(0, seohyun.substitute());
        assertEquals("0%", seohyun.rate());

        // 예손: assigned=0, selfDone=0, missed=0, substitute=1, rate=null
        assertEquals(0, yeson.assigned());
        assertEquals(0, yeson.selfDone());
        assertEquals(0, yeson.missed());
        assertEquals(1, yeson.substitute());
        assertNull(yeson.rate());
    }

    @Test
    void getStats_담당없는_룸메이트는_rate_null() {
        InMemoryDutyRepository repo = new InMemoryDutyRepository();
        // 예손에게는 duty 없음

        StatsService service = buildService(repo);
        StatsResponse response = service.getStats();

        MemberStat yeson = response.members().stream()
                .filter(m -> m.memberId().equals(YESON))
                .findFirst().orElseThrow();

        assertNull(yeson.rate());
        assertEquals(0, yeson.assigned());
    }

    @Test
    void getStats_순번대로_정렬() {
        InMemoryDutyRepository repo = new InMemoryDutyRepository();

        StatsService service = buildService(repo);
        StatsResponse response = service.getStats();
        List<MemberStat> members = response.members();

        assertEquals(4, members.size());
        assertEquals(MINJEONG, members.get(0).memberId());
        assertEquals("민정",   members.get(0).name());
        assertEquals(JIHYUN,   members.get(1).memberId());
        assertEquals("지현",   members.get(1).name());
        assertEquals(SEOHYUN,  members.get(2).memberId());
        assertEquals("서현",   members.get(2).name());
        assertEquals(YESON,    members.get(3).memberId());
        assertEquals("예손",   members.get(3).name());
    }

    @Test
    void getStats_비활성_멤버의_기록이_결과에_포함() {
        // 비활성 멤버가 있는 roster 사용
        RosterClient rosterWithInactive = () -> new com.example.cleanmate.domain.client.RosterResponse(
                ROOM_ID,
                "525호",
                LocalDate.of(2026, 11, 2),
                List.of(
                        new com.example.cleanmate.domain.client.RosterResponse.MemberInfo(MINJEONG, "민정", true),
                        new com.example.cleanmate.domain.client.RosterResponse.MemberInfo(JIHYUN,   "지현", true),
                        new com.example.cleanmate.domain.client.RosterResponse.MemberInfo(SEOHYUN,  "서현", true),
                        new com.example.cleanmate.domain.client.RosterResponse.MemberInfo(YESON,    "예손", false)  // 비활성
                ),
                List.of(
                        new com.example.cleanmate.domain.client.RosterResponse.ZoneInfo(ZONE_1, "샤워실", true),
                        new com.example.cleanmate.domain.client.RosterResponse.ZoneInfo(ZONE_2, "화장실", true),
                        new com.example.cleanmate.domain.client.RosterResponse.ZoneInfo(ZONE_3, "세면대", true),
                        new com.example.cleanmate.domain.client.RosterResponse.ZoneInfo(ZONE_4, "바닥",   true)
                )
        );

        InMemoryDutyRepository repo = new InMemoryDutyRepository();
        // 비활성 멤버(예손)가 과거에 청소한 기록
        Duty yesonDone = repo.save(Duty.newAssignment(ROOM_ID, LAST_WEEK, ZONE_4, YESON));
        yesonDone.markDone(YESON, LAST_WEEK.plusDays(1));
        repo.save(yesonDone);

        StatsService svc = new StatsService(repo, rosterWithInactive, new WeekCalendar(FIXED_CLOCK));
        StatsResponse response = svc.getStats();

        // 비활성 멤버도 기록이 있으면 결과에 포함
        MemberStat yeson = response.members().stream()
                .filter(m -> m.memberId().equals(YESON))
                .findFirst().orElseThrow(() -> new AssertionError("예손이 결과에 없음"));

        assertEquals(1, yeson.assigned());
        assertEquals(1, yeson.selfDone());
    }
}
