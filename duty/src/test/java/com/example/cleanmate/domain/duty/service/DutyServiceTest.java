package com.example.cleanmate.domain.duty.service;

import com.example.cleanmate.domain.client.StubRosterClient;
import com.example.cleanmate.domain.duty.dto.WeekDutyResponse;
import com.example.cleanmate.domain.duty.exception.AlreadyDoneException;
import com.example.cleanmate.domain.duty.exception.DutyNotFoundException;
import com.example.cleanmate.domain.duty.exception.NotAssigneeException;
import com.example.cleanmate.domain.duty.exception.NotRecordOwnerException;
import com.example.cleanmate.domain.duty.model.Duty;
import com.example.cleanmate.domain.duty.model.DutyStatus;
import com.example.cleanmate.domain.duty.model.RotationPolicy;
import com.example.cleanmate.domain.duty.model.WeekCalendar;
import com.example.cleanmate.domain.duty.repository.DutyRepository;
import com.example.cleanmate.domain.duty.repository.InMemoryDutyRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class DutyServiceTest {

    private static final ZoneId SEOUL = ZoneId.of("Asia/Seoul");

    // 고정 날짜: 2026-11-05(목) → currentMonday = 2026-11-02
    private static final Clock FIXED_CLOCK =
            Clock.fixed(LocalDate.of(2026, 11, 5).atStartOfDay(SEOUL).toInstant(), SEOUL);

    // StubRosterClient 예시 데이터 기준
    // 샤워실(1L) → 민정(1L), 화장실(2L) → 지현(2L), 세면대(3L) → 서현(3L), 바닥(4L) → 예손(4L)
    private static final Long ZONE_샤워실  = 1L;
    private static final Long ZONE_바닥    = 4L;
    private static final Long MEMBER_민정  = 1L;  // 샤워실 담당
    private static final Long MEMBER_지현  = 2L;  // 화장실 담당
    private static final Long MEMBER_예손  = 4L;  // 바닥 담당

    private DutyService service;

    @BeforeEach
    void setUp() {
        service = new DutyService(
                new InMemoryDutyRepository(),
                new StubRosterClient(),
                new WeekCalendar(FIXED_CLOCK),
                new RotationPolicy(),
                event -> {}  // no-op: 단위 테스트에서 이벤트 발행 불필요
        );
    }

    // ── getWeekDuties ─────────────────────────────────────────

    @Test
    void getWeekDuties_배정이_없으면_자동생성() {
        WeekDutyResponse result = service.getWeekDuties();
        assertEquals(LocalDate.of(2026, 11, 2), result.weekStart());
        assertEquals(4, result.duties().size());
    }

    @Test
    void getWeekDuties_두번_호출해도_배정은_한번만_생성() {
        WeekDutyResponse first  = service.getWeekDuties();
        WeekDutyResponse second = service.getWeekDuties();
        assertEquals(first.duties().size(), second.duties().size());
        assertEquals(first.duties().get(0).zoneId(), second.duties().get(0).zoneId());
    }

    @Test
    void getWeekDuties_11월2주_순환_배정_확인() {
        WeekDutyResponse result = service.getWeekDuties();
        assertEquals("민정", findByZone(result, "샤워실").assigneeName());
        assertEquals("지현", findByZone(result, "화장실").assigneeName());
        assertEquals("서현", findByZone(result, "세면대").assigneeName());
        assertEquals("예손", findByZone(result, "바닥").assigneeName());
    }

    @Test
    void getWeekDuties_초기상태는_전부_PENDING() {
        service.getWeekDuties().duties()
                .forEach(d -> assertEquals(DutyStatus.PENDING, d.status()));
    }

    // ── markDone ─────────────────────────────────────────────

    @Test
    void markDone_담당자가_완료하면_DONE() {
        service.getWeekDuties(); // 배정 생성
        service.markDone(ZONE_샤워실, MEMBER_민정);

        WeekDutyResponse result = service.getWeekDuties();
        WeekDutyResponse.DutyItem item = findByZone(result, "샤워실");
        assertEquals(DutyStatus.DONE, item.status());
        assertEquals(MEMBER_민정, item.doneById());
        assertFalse(item.doneByName() != null && !item.doneByName().equals(item.assigneeName())); // 본인 완료
    }

    @Test
    void markDone_담당자가_아니면_NotAssigneeException() {
        service.getWeekDuties();
        // 샤워실 담당은 민정(1L), 지현(2L)이 /done 시도
        assertThrows(NotAssigneeException.class,
                () -> service.markDone(ZONE_샤워실, MEMBER_지현));
    }

    @Test
    void markDone_이미_완료된_구역은_AlreadyDoneException() {
        service.getWeekDuties();
        service.markDone(ZONE_샤워실, MEMBER_민정);
        assertThrows(AlreadyDoneException.class,
                () -> service.markDone(ZONE_샤워실, MEMBER_민정));
    }

    @Test
    void markDone_동시_완료_경쟁에서_updateIfStatus_false이면_AlreadyDoneException() {
        // updateIfStatus가 항상 false를 반환하는 저장소로 동시 완료 패배 상황 재현
        DutyRepository neverUpdates = new InMemoryDutyRepository() {
            @Override
            public boolean updateIfStatus(Duty duty, DutyStatus requiredCurrentStatus) {
                return false;  // 다른 요청이 먼저 완료한 상황
            }
        };
        DutyService raceService = new DutyService(
                neverUpdates,
                new StubRosterClient(),
                new WeekCalendar(FIXED_CLOCK),
                new RotationPolicy(),
                event -> {}
        );
        raceService.getWeekDuties();  // 배정 생성

        assertThrows(AlreadyDoneException.class,
                () -> raceService.markDone(ZONE_샤워실, MEMBER_민정));
    }

    @Test
    void markDone_없는_구역은_DutyNotFoundException() {
        service.getWeekDuties();
        assertThrows(DutyNotFoundException.class,
                () -> service.markDone(999L, MEMBER_민정));
    }

    // ── markSubstitute ───────────────────────────────────────

    @Test
    void markSubstitute_대신청소하면_DONE이고_isSubstitute() {
        service.getWeekDuties();
        // 바닥(4L) 담당은 예손(4L). 민정(1L)이 대신 청소
        service.markSubstitute(ZONE_바닥, MEMBER_민정);

        WeekDutyResponse result = service.getWeekDuties();
        WeekDutyResponse.DutyItem item = findByZone(result, "바닥");
        assertEquals(DutyStatus.DONE, item.status());
        assertEquals(MEMBER_민정, item.doneById());
        assertEquals(MEMBER_예손, item.assigneeId()); // 담당자는 그대로
    }

    @Test
    void markSubstitute_본인_담당에_substitute하면_NotAssigneeException() {
        service.getWeekDuties();
        // 샤워실 담당이 민정인데 민정이 substitute 시도
        assertThrows(NotAssigneeException.class,
                () -> service.markSubstitute(ZONE_샤워실, MEMBER_민정));
    }

    @Test
    void markSubstitute_이미_완료된_구역은_AlreadyDoneException() {
        service.getWeekDuties();
        service.markDone(ZONE_샤워실, MEMBER_민정); // 먼저 완료
        assertThrows(AlreadyDoneException.class,
                () -> service.markSubstitute(ZONE_샤워실, MEMBER_지현));
    }

    // ── cancelRecord ─────────────────────────────────────────

    @Test
    void cancelRecord_본인_기록_취소하면_PENDING() {
        service.getWeekDuties();
        service.markDone(ZONE_샤워실, MEMBER_민정);
        service.cancelRecord(ZONE_샤워실, MEMBER_민정);

        WeekDutyResponse result = service.getWeekDuties();
        assertEquals(DutyStatus.PENDING, findByZone(result, "샤워실").status());
    }

    @Test
    void cancelRecord_타인_기록은_NotRecordOwnerException() {
        service.getWeekDuties();
        service.markDone(ZONE_샤워실, MEMBER_민정);
        // 지현이 민정의 기록을 취소 시도
        assertThrows(NotRecordOwnerException.class,
                () -> service.cancelRecord(ZONE_샤워실, MEMBER_지현));
    }

    @Test
    void cancelRecord_PENDING_상태는_취소_불가() {
        service.getWeekDuties();
        assertThrows(AlreadyDoneException.class,
                () -> service.cancelRecord(ZONE_샤워실, MEMBER_민정));
    }

    // ── closePastWeeks ────────────────────────────────────────

    /**
     * 고정 날짜: 2026-11-16(월) → currentMonday = 2026-11-16
     * StubRosterClient.startWeek() = 2026-11-02
     * 따라서 11/2 주와 11/9 주가 "지난 주"
     */
    private static final Clock CLOCK_1116 =
            Clock.fixed(LocalDate.of(2026, 11, 16).atStartOfDay(SEOUL).toInstant(), SEOUL);

    @Test
    void closePastWeeks_지난주_PENDING이_MISSED로() {
        InMemoryDutyRepository repo = new InMemoryDutyRepository();
        DutyService svc = new DutyService(
                repo,
                new StubRosterClient(),
                new WeekCalendar(CLOCK_1116),
                new RotationPolicy(),
                event -> {}
        );

        // closePastWeeks 실행 — 11/2, 11/9 주 배정 생성 후 MISSED로 확정, 11/16(현재 주) PENDING 생성
        svc.closePastWeeks();

        // 11/2 주와 11/9 주의 모든 duty가 MISSED여야 한다
        LocalDate week1 = LocalDate.of(2026, 11, 2);
        LocalDate week2 = LocalDate.of(2026, 11, 9);
        List<Duty> allDuties = repo.findByRoom(1L);
        assertFalse(allDuties.isEmpty());
        // 11/16 주(현재 주) 배정은 생성되지만 PENDING 상태여야 한다
        LocalDate currentWeek = LocalDate.of(2026, 11, 16);
        assertTrue(allDuties.stream().anyMatch(d -> d.getWeekStart().equals(currentWeek)));
        allDuties.stream()
                .filter(d -> d.getWeekStart().equals(currentWeek))
                .forEach(d -> assertEquals(DutyStatus.PENDING, d.getStatus()));
        // 지난 주 배정은 모두 MISSED
        allDuties.stream()
                .filter(d -> d.getWeekStart().isBefore(currentWeek))
                .forEach(d -> assertEquals(DutyStatus.MISSED, d.getStatus(),
                        "주 " + d.getWeekStart() + " zone " + d.getZoneId() + "이 MISSED여야 합니다."));
    }

    @Test
    void closePastWeeks_두번_실행해도_결과_같음() {
        InMemoryDutyRepository repo = new InMemoryDutyRepository();
        DutyService svc = new DutyService(
                repo,
                new StubRosterClient(),
                new WeekCalendar(CLOCK_1116),
                new RotationPolicy(),
                event -> {}
        );

        svc.closePastWeeks();
        svc.closePastWeeks();  // 두 번째 실행

        List<Duty> allDuties = repo.findByRoom(1L);
        LocalDate currentWeek = LocalDate.of(2026, 11, 16);
        // 지난 주 배정은 모두 MISSED
        allDuties.stream()
                .filter(d -> d.getWeekStart().isBefore(currentWeek))
                .forEach(d -> assertEquals(DutyStatus.MISSED, d.getStatus()));
        // 현재 주 배정은 PENDING
        allDuties.stream()
                .filter(d -> d.getWeekStart().equals(currentWeek))
                .forEach(d -> assertEquals(DutyStatus.PENDING, d.getStatus()));
    }

    @Test
    void closePastWeeks_빈_주_배정_생성_후_확정() {
        InMemoryDutyRepository repo = new InMemoryDutyRepository();
        DutyService svc = new DutyService(
                repo,
                new StubRosterClient(),
                new WeekCalendar(CLOCK_1116),
                new RotationPolicy(),
                event -> {}
        );

        // 실행 전에는 배정 없음
        assertTrue(repo.findByRoom(1L).isEmpty());

        svc.closePastWeeks();

        // 실행 후: 11/2, 11/9, 11/16 각각 4개 구역 = 12개 duty
        List<Duty> allDuties = repo.findByRoom(1L);
        LocalDate currentWeek = LocalDate.of(2026, 11, 16);
        assertEquals(12, allDuties.size());
        allDuties.stream()
                .filter(d -> d.getWeekStart().isBefore(currentWeek))
                .forEach(d -> assertEquals(DutyStatus.MISSED, d.getStatus()));
        allDuties.stream()
                .filter(d -> d.getWeekStart().equals(currentWeek))
                .forEach(d -> assertEquals(DutyStatus.PENDING, d.getStatus()));
    }

    // ── correctLastWeek ───────────────────────────────────────

    // 고정 날짜: 2026-11-10(화) → currentMonday = 2026-11-09, lastMonday = 2026-11-02
    private static final Clock CLOCK_1110 =
            Clock.fixed(LocalDate.of(2026, 11, 10).atStartOfDay(SEOUL).toInstant(), SEOUL);

    private DutyService buildServiceWithClock(Clock clock) {
        return new DutyService(
                new InMemoryDutyRepository(),
                new StubRosterClient(),
                new WeekCalendar(clock),
                new RotationPolicy(),
                event -> {}
        );
    }

    @Test
    void correctLastWeek_정상_DONE_보정() {
        InMemoryDutyRepository repo = new InMemoryDutyRepository();
        DutyService svc = new DutyService(repo, new StubRosterClient(),
                new WeekCalendar(CLOCK_1110), new RotationPolicy(), event -> {});

        // 지난 주(11/2) 배정 생성
        svc.getWeekDuties(); // 이번 주(11/9) 배정도 생성되지만 무관
        // 11/2 주를 직접 채워야 함
        InMemoryDutyRepository repo2 = new InMemoryDutyRepository();
        DutyService svcLastWeek = new DutyService(repo2, new StubRosterClient(),
                new WeekCalendar(CLOCK_1116), new RotationPolicy(), event -> {});
        svcLastWeek.closePastWeeks(); // 11/2, 11/9 배정 채우고 MISSED로

        // repo2의 11/2 주 샤워실(1L) 기록을 DONE으로 보정
        com.example.cleanmate.domain.duty.dto.CorrectLastWeekRequest req =
                new com.example.cleanmate.domain.duty.dto.CorrectLastWeekRequest(
                        1L, "DONE",
                        LocalDate.of(2026, 11, 2),
                        1L,
                        LocalDate.of(2026, 11, 3)
                );

        DutyService corrSvc = new DutyService(repo2, new StubRosterClient(),
                new WeekCalendar(CLOCK_1110), new RotationPolicy(), event -> {});

        corrSvc.correctLastWeek(4L, "ADMIN", 1L, req);

        List<Duty> duties = repo2.findByRoom(1L);
        Duty corrected = duties.stream()
                .filter(d -> d.getWeekStart().equals(LocalDate.of(2026, 11, 2))
                        && d.getZoneId().equals(1L))
                .findFirst().orElseThrow();
        assertEquals(DutyStatus.DONE, corrected.getStatus());
        assertEquals(1L, corrected.getDoneById());
    }

    @Test
    void correctLastWeek_비관리자_403() {
        InMemoryDutyRepository repo = new InMemoryDutyRepository();
        DutyService svc = new DutyService(repo, new StubRosterClient(),
                new WeekCalendar(CLOCK_1116), new RotationPolicy(), event -> {});
        svc.closePastWeeks();

        com.example.cleanmate.domain.duty.dto.CorrectLastWeekRequest req =
                new com.example.cleanmate.domain.duty.dto.CorrectLastWeekRequest(
                        1L, "MISSED", LocalDate.of(2026, 11, 9), null, null
                );

        assertThrows(com.example.cleanmate.global.exception.AdminOnlyException.class,
                () -> svc.correctLastWeek(1L, "MEMBER", 1L, req));
    }

    @Test
    void correctLastWeek_다른_주_요청_400() {
        InMemoryDutyRepository repo = new InMemoryDutyRepository();
        DutyService svc = new DutyService(repo, new StubRosterClient(),
                new WeekCalendar(CLOCK_1116), new RotationPolicy(), event -> {});
        svc.closePastWeeks();

        // 11/9 (이번 주)를 보정 요청 — lastMonday는 11/9이므로 맞음
        // 하지만 CLOCK_1116에서 lastMonday = 11/9, 그러면 11/9는 맞는 것
        // 11/2를 lastMonday로 갖는 clock으로 11/9 주를 보정 시도
        // CLOCK_1116: currentMonday=11/16, lastMonday=11/9
        com.example.cleanmate.domain.duty.dto.CorrectLastWeekRequest req =
                new com.example.cleanmate.domain.duty.dto.CorrectLastWeekRequest(
                        1L, "MISSED",
                        LocalDate.of(2026, 11, 2),  // lastMonday는 11/9인데 11/2 요청
                        null, null
                );

        com.example.cleanmate.global.exception.CleanmateException ex =
                assertThrows(com.example.cleanmate.global.exception.CleanmateException.class,
                        () -> svc.correctLastWeek(4L, "ADMIN", 1L, req));
        assertEquals(400, ex.getStatus());
    }

    @Test
    void correctLastWeek_날짜_범위_밖_400() {
        InMemoryDutyRepository repo = new InMemoryDutyRepository();
        DutyService svc = new DutyService(repo, new StubRosterClient(),
                new WeekCalendar(CLOCK_1116), new RotationPolicy(), event -> {});
        svc.closePastWeeks();

        // 완료일이 지난 주(11/9~11/15) 범위를 벗어남 (11/8 = 전 주 일요일)
        com.example.cleanmate.domain.duty.dto.CorrectLastWeekRequest req =
                new com.example.cleanmate.domain.duty.dto.CorrectLastWeekRequest(
                        1L, "DONE",
                        LocalDate.of(2026, 11, 9),
                        1L,
                        LocalDate.of(2026, 11, 8)  // 범위 밖
                );

        com.example.cleanmate.global.exception.CleanmateException ex =
                assertThrows(com.example.cleanmate.global.exception.CleanmateException.class,
                        () -> svc.correctLastWeek(4L, "ADMIN", 1L, req));
        assertEquals(400, ex.getStatus());
    }

    // ── finalizeRoom ──────────────────────────────────────────

    @Test
    void finalizeRoom_이번주_PENDING도_MISSED로() {
        InMemoryDutyRepository repo = new InMemoryDutyRepository();
        // CLOCK_1110: currentMonday = 2026-11-09
        DutyService svc = new DutyService(repo, new StubRosterClient(),
                new WeekCalendar(CLOCK_1110), new RotationPolicy(), event -> {});

        // 이번 주 배정 생성
        svc.getWeekDuties();

        svc.finalizeRoom(1L);

        List<Duty> duties = repo.findByRoom(1L);
        assertFalse(duties.isEmpty());
        // 모든 duty가 MISSED여야 함
        duties.forEach(d -> assertEquals(DutyStatus.MISSED, d.getStatus(),
                "주 " + d.getWeekStart() + " zone " + d.getZoneId() + "이 MISSED여야 합니다."));
    }

    // ── 헬퍼 ─────────────────────────────────────────────────

    private WeekDutyResponse.DutyItem findByZone(WeekDutyResponse response, String zoneName) {
        return response.duties().stream()
                .filter(d -> d.zoneName().equals(zoneName))
                .findFirst()
                .orElseThrow(() -> new AssertionError("구역 없음: " + zoneName));
    }
}
