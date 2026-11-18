package com.example.cleanmate.domain.schedule;

import com.example.cleanmate.domain.client.StubRosterClient;
import com.example.cleanmate.domain.duty.model.RotationPolicy;
import com.example.cleanmate.domain.duty.model.WeekCalendar;
import com.example.cleanmate.domain.duty.repository.InMemoryDutyRepository;
import com.example.cleanmate.domain.duty.service.DutyService;
import com.example.cleanmate.global.lock.JobLock;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.LocalDate;
import java.time.ZoneId;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 정기 작업이 잠금을 얻은 경우에만 실행되는지 시험한다.
 * Redis 대신 메모리 변수 하나로 잠금을 흉내 내는 가짜 JobLock을 쓴다.
 * 두 WeeklyJob(duty-1, duty-2)이 같은 잠금을 공유하는 상황을 만든다.
 */
class WeeklyJobTest {

    private static final ZoneId SEOUL = ZoneId.of("Asia/Seoul");
    // 2026-11-10(화) → 이번 주 11/9, 시작 주 11/2 (StubRosterClient)
    private static final Clock CLOCK = Clock.fixed(LocalDate.of(2026, 11, 10).atStartOfDay(SEOUL).toInstant(), SEOUL);

    /** 여러 서버가 공유하는 가짜 잠금: 누가 가지고 있는지만 기억 */
    static class SharedLock {
        String owner;
    }

    /** 서버 한 대의 잠금 손잡이 */
    static class FakeJobLock implements JobLock {
        private final SharedLock shared;
        private final String me;
        int unlockCount;

        FakeJobLock(SharedLock shared, String me) {
            this.shared = shared;
            this.me = me;
        }

        @Override
        public boolean tryLock() {
            if (shared.owner != null) return false;   // 이미 누가 가지고 있음 (SET NX 실패)
            shared.owner = me;
            return true;
        }

        @Override
        public void unlock() {
            unlockCount++;
            if (me.equals(shared.owner)) shared.owner = null;   // 내 것일 때만 해제
        }
    }

    private DutyService newService(InMemoryDutyRepository repo) {
        return new DutyService(repo, new StubRosterClient(), new WeekCalendar(CLOCK), new RotationPolicy(), e -> {});
    }

    @Test
    void 잠금을_얻은_서버만_작업하고_끝나면_잠금을_푼다() {
        InMemoryDutyRepository repo = new InMemoryDutyRepository();
        SharedLock shared = new SharedLock();
        FakeJobLock lock1 = new FakeJobLock(shared, "duty-1");
        WeeklyJob job1 = new WeeklyJob(newService(repo), lock1, "duty-1");

        job1.execute("test");

        // 11/2 주, 11/9 주 배정이 모두 만들어짐 (구역 4개 × 2주)
        assertEquals(8, repo.findByRoom(1L).size());
        assertNull(shared.owner, "작업이 끝나면 잠금이 풀려야 한다");
        assertEquals(1, lock1.unlockCount);
    }

    @Test
    void 다른_서버가_잠금을_가지고_있으면_건너뛰고_그_잠금을_건드리지_않는다() {
        InMemoryDutyRepository repo = new InMemoryDutyRepository();
        SharedLock shared = new SharedLock();
        shared.owner = "duty-1";   // duty-1이 실행 중
        FakeJobLock lock2 = new FakeJobLock(shared, "duty-2");
        WeeklyJob job2 = new WeeklyJob(newService(repo), lock2, "duty-2");

        job2.execute("test");

        assertEquals(0, repo.findByRoom(1L).size(), "잠금을 못 얻은 서버는 배정을 만들지 않는다");
        assertEquals("duty-1", shared.owner, "다른 서버의 잠금은 그대로");
        assertEquals(0, lock2.unlockCount, "얻지 못한 잠금은 풀지 않는다");
    }

    @Test
    void 잠금_확인이_실패하면_작업하지_않는다() {
        InMemoryDutyRepository repo = new InMemoryDutyRepository();
        JobLock broken = new JobLock() {
            @Override public boolean tryLock() { throw new IllegalStateException("Redis 연결 실패"); }
            @Override public void unlock() { fail("잠금을 얻지 못했으면 풀지 않는다"); }
        };
        WeeklyJob job = new WeeklyJob(newService(repo), broken, "duty-1");

        assertDoesNotThrow(() -> job.execute("test"));
        assertEquals(0, repo.findByRoom(1L).size());
    }
}
