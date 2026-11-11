package com.example.cleanmate.domain.duty.repository;

import com.example.cleanmate.domain.duty.model.Duty;
import com.example.cleanmate.domain.duty.model.DutyStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

/**
 * DutyRepository 구현체가 반드시 만족해야 할 계약(LSP).
 * InMemoryDutyRepository 와 JdbcDutyRepository 가 이 클래스를 상속한다.
 */
abstract class DutyRepositoryContractTest {

    protected abstract DutyRepository createRepository();

    private DutyRepository repo;

    private static final Long ROOM_ID   = 1L;
    private static final Long ZONE_1    = 1L;
    private static final Long ZONE_2    = 2L;
    private static final Long ASSIGNEE  = 10L;
    private static final LocalDate WEEK = LocalDate.of(2026, 11, 2);

    @BeforeEach
    void init() {
        repo = createRepository();
    }

    @Test
    void save_insert_새_id가_부여된다() {
        Duty duty = Duty.newAssignment(ROOM_ID, WEEK, ZONE_1, ASSIGNEE);
        Duty saved = repo.save(duty);

        assertNotNull(saved.getId());
        assertEquals(DutyStatus.PENDING, saved.getStatus());
    }

    @Test
    void findByRoomAndWeekAndZone_존재하면_반환한다() {
        Duty saved = repo.save(Duty.newAssignment(ROOM_ID, WEEK, ZONE_1, ASSIGNEE));

        Optional<Duty> found = repo.findByRoomAndWeekAndZone(ROOM_ID, WEEK, ZONE_1);

        assertTrue(found.isPresent());
        assertEquals(saved.getId(), found.get().getId());
    }

    @Test
    void findByRoomAndWeekAndZone_없으면_empty를_반환한다() {
        Optional<Duty> found = repo.findByRoomAndWeekAndZone(ROOM_ID, WEEK, ZONE_1);
        assertTrue(found.isEmpty());
    }

    @Test
    void findByRoomAndWeek_해당_주_전체를_반환한다() {
        repo.save(Duty.newAssignment(ROOM_ID, WEEK, ZONE_1, ASSIGNEE));
        repo.save(Duty.newAssignment(ROOM_ID, WEEK, ZONE_2, ASSIGNEE));

        List<Duty> duties = repo.findByRoomAndWeek(ROOM_ID, WEEK);

        assertEquals(2, duties.size());
    }

    @Test
    void findByRoomAndWeek_다른_주는_포함하지_않는다() {
        LocalDate otherWeek = WEEK.plusWeeks(1);
        repo.save(Duty.newAssignment(ROOM_ID, WEEK, ZONE_1, ASSIGNEE));
        repo.save(Duty.newAssignment(ROOM_ID, otherWeek, ZONE_2, ASSIGNEE));

        List<Duty> duties = repo.findByRoomAndWeek(ROOM_ID, WEEK);

        assertEquals(1, duties.size());
        assertEquals(ZONE_1, duties.get(0).getZoneId());
    }

    @Test
    void existsByRoomAndWeek_존재하면_true() {
        repo.save(Duty.newAssignment(ROOM_ID, WEEK, ZONE_1, ASSIGNEE));
        assertTrue(repo.existsByRoomAndWeek(ROOM_ID, WEEK));
    }

    @Test
    void existsByRoomAndWeek_없으면_false() {
        assertFalse(repo.existsByRoomAndWeek(ROOM_ID, WEEK));
    }

    @Test
    void save_update_상태변경이_반영된다() {
        Duty saved = repo.save(Duty.newAssignment(ROOM_ID, WEEK, ZONE_1, ASSIGNEE));
        saved.markDone(ASSIGNEE, WEEK.plusDays(2));
        repo.save(saved);

        Duty updated = repo.findByRoomAndWeekAndZone(ROOM_ID, WEEK, ZONE_1).orElseThrow();

        assertEquals(DutyStatus.DONE, updated.getStatus());
        assertEquals(ASSIGNEE, updated.getDoneById());
    }

    /**
     * LSP 방어적 복사 검증 — JDBC 는 매번 새 객체를 주므로 항상 통과.
     * InMemory 도 조회 시 복사본을 주어야 같은 결과가 나와야 한다.
     */
    @Test
    void 조회한_객체를_수정해도_다시_조회하면_원본_상태를_반환한다() {
        repo.save(Duty.newAssignment(ROOM_ID, WEEK, ZONE_1, ASSIGNEE));

        // 꺼낸 객체에 markDone — save 없이 메모리만 바꿈
        Duty fetched = repo.findByRoomAndWeekAndZone(ROOM_ID, WEEK, ZONE_1).orElseThrow();
        fetched.markDone(ASSIGNEE, WEEK.plusDays(1));

        // 다시 조회하면 여전히 PENDING 이어야 한다
        Duty again = repo.findByRoomAndWeekAndZone(ROOM_ID, WEEK, ZONE_1).orElseThrow();
        assertEquals(DutyStatus.PENDING, again.getStatus(),
                "조회한 객체를 바꿔도 저장소 내부 상태는 변하지 않아야 합니다 (방어적 복사).");
    }

    @Test
    void updateIfStatus_기대_상태_일치하면_갱신하고_true_반환() {
        Duty saved = repo.save(Duty.newAssignment(ROOM_ID, WEEK, ZONE_1, ASSIGNEE));
        saved.markDone(ASSIGNEE, WEEK.plusDays(1));

        boolean result = repo.updateIfStatus(saved, DutyStatus.PENDING);

        assertTrue(result);
        Duty found = repo.findByRoomAndWeekAndZone(ROOM_ID, WEEK, ZONE_1).orElseThrow();
        assertEquals(DutyStatus.DONE, found.getStatus());
    }

    @Test
    void findByRoom_해당_방_배정만_반환() {
        Long room2 = 2L;
        // 방 1L의 duty 2개
        repo.save(Duty.newAssignment(ROOM_ID, WEEK, ZONE_1, ASSIGNEE));
        repo.save(Duty.newAssignment(ROOM_ID, WEEK, ZONE_2, ASSIGNEE));
        // 방 2L의 duty 1개
        repo.save(Duty.newAssignment(room2, WEEK, ZONE_1, ASSIGNEE));

        List<Duty> room1Duties = repo.findByRoom(ROOM_ID);
        List<Duty> room2Duties = repo.findByRoom(room2);

        assertEquals(2, room1Duties.size());
        assertEquals(1, room2Duties.size());
        assertTrue(room1Duties.stream().allMatch(d -> d.getRoomId().equals(ROOM_ID)));
        assertTrue(room2Duties.stream().allMatch(d -> d.getRoomId().equals(room2)));
    }

    @Test
    void updateIfStatus_기대_상태_불일치하면_false_반환() {
        Duty saved = repo.save(Duty.newAssignment(ROOM_ID, WEEK, ZONE_1, ASSIGNEE));
        saved.markDone(ASSIGNEE, WEEK.plusDays(1));
        repo.save(saved);  // DB/store 에 DONE 으로 저장

        // 다시 DONE → PENDING 으로 취소 시도 — 그런데 기대 상태를 PENDING 으로 잘못 넘김
        saved.cancel();
        boolean result = repo.updateIfStatus(saved, DutyStatus.PENDING);  // 현재 DONE 이므로 불일치

        assertFalse(result);
    }

    @Test
    void markMissedBefore_지난주_PENDING은_MISSED로_이번주와_DONE은_그대로() {
        LocalDate lastWeek    = LocalDate.of(2026, 11, 2);
        LocalDate currentWeek = LocalDate.of(2026, 11, 9);

        Duty past = repo.save(Duty.newAssignment(ROOM_ID, lastWeek, ZONE_1, 1L));
        repo.save(Duty.newAssignment(ROOM_ID, currentWeek, ZONE_2, 2L));
        Duty done = repo.save(Duty.newAssignment(ROOM_ID, lastWeek, 3L, 3L));

        // done을 DONE 상태로 갱신
        done.markDone(3L, lastWeek.plusDays(1));
        repo.save(done);

        int updated = repo.markMissedBefore(ROOM_ID, currentWeek);

        assertEquals(1, updated);
        assertEquals(DutyStatus.MISSED,  repo.findByRoomAndWeekAndZone(ROOM_ID, lastWeek,    ZONE_1).orElseThrow().getStatus());
        assertEquals(DutyStatus.PENDING, repo.findByRoomAndWeekAndZone(ROOM_ID, currentWeek, ZONE_2).orElseThrow().getStatus());
        assertEquals(DutyStatus.DONE,    repo.findByRoomAndWeekAndZone(ROOM_ID, lastWeek,    3L).orElseThrow().getStatus());
    }
}
