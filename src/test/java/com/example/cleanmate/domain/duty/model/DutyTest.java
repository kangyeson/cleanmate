package com.example.cleanmate.domain.duty.model;

import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.*;

class DutyTest {

    // 고정 데이터
    private static final Long ROOM_ID     = 1L;
    private static final LocalDate WEEK   = LocalDate.of(2026, 11, 2); // 11/2(월)
    private static final Long ZONE_ID     = 10L; // 샤워실
    private static final Long ASSIGNEE    = 1L;  // 민정 (담당자)
    private static final Long OTHER       = 2L;  // 지현 (대신 청소할 사람)
    private static final LocalDate TODAY  = LocalDate.of(2026, 11, 3);

    private Duty pendingDuty() {
        return Duty.newAssignment(ROOM_ID, WEEK, ZONE_ID, ASSIGNEE);
    }

    // ── newAssignment ────────────────────────────────────────

    @Test
    void newAssignment_초기상태는_PENDING() {
        Duty duty = pendingDuty();
        assertEquals(DutyStatus.PENDING, duty.getStatus());
        assertNull(duty.getId());
        assertNull(duty.getDoneById());
        assertNull(duty.getDoneDate());
    }

    // ── markDone ─────────────────────────────────────────────

    @Test
    void markDone_PENDING에서_DONE으로() {
        Duty duty = pendingDuty();
        duty.markDone(ASSIGNEE, TODAY);

        assertEquals(DutyStatus.DONE, duty.getStatus());
        assertEquals(ASSIGNEE, duty.getDoneById());
        assertEquals(TODAY, duty.getDoneDate());
    }

    @Test
    void markDone_DONE상태에서_예외() {
        Duty duty = pendingDuty();
        duty.markDone(ASSIGNEE, TODAY);

        assertThrows(IllegalStateException.class, () -> duty.markDone(ASSIGNEE, TODAY));
    }

    @Test
    void markDone_MISSED상태에서_예외() {
        Duty duty = pendingDuty();
        duty.markMissed();

        assertThrows(IllegalStateException.class, () -> duty.markDone(ASSIGNEE, TODAY));
    }

    // ── cancel ───────────────────────────────────────────────

    @Test
    void cancel_DONE에서_PENDING으로_doneBy_초기화() {
        Duty duty = pendingDuty();
        duty.markDone(ASSIGNEE, TODAY);
        duty.cancel();

        assertEquals(DutyStatus.PENDING, duty.getStatus());
        assertNull(duty.getDoneById());
        assertNull(duty.getDoneDate());
    }

    @Test
    void cancel_PENDING상태에서_예외() {
        Duty duty = pendingDuty();
        assertThrows(IllegalStateException.class, duty::cancel);
    }

    @Test
    void cancel_MISSED상태에서_예외() {
        Duty duty = pendingDuty();
        duty.markMissed();
        assertThrows(IllegalStateException.class, duty::cancel);
    }

    // ── markMissed ───────────────────────────────────────────

    @Test
    void markMissed_PENDING에서_MISSED로() {
        Duty duty = pendingDuty();
        duty.markMissed();
        assertEquals(DutyStatus.MISSED, duty.getStatus());
    }

    @Test
    void markMissed_DONE상태에서_예외() {
        Duty duty = pendingDuty();
        duty.markDone(ASSIGNEE, TODAY);
        assertThrows(IllegalStateException.class, duty::markMissed);
    }

    @Test
    void markMissed_MISSED상태에서_예외() {
        Duty duty = pendingDuty();
        duty.markMissed();
        assertThrows(IllegalStateException.class, duty::markMissed);
    }

    // ── isSubstitute ─────────────────────────────────────────

    @Test
    void isSubstitute_본인이_청소하면_false() {
        Duty duty = pendingDuty();
        duty.markDone(ASSIGNEE, TODAY);
        assertFalse(duty.isSubstitute());
    }

    @Test
    void isSubstitute_다른사람이_청소하면_true() {
        Duty duty = pendingDuty();
        duty.markDone(OTHER, TODAY);
        assertTrue(duty.isSubstitute());
    }

    @Test
    void isSubstitute_PENDING이면_false() {
        Duty duty = pendingDuty();
        assertFalse(duty.isSubstitute());
    }

    @Test
    void isSubstitute_MISSED이면_false() {
        Duty duty = pendingDuty();
        duty.markMissed();
        assertFalse(duty.isSubstitute());
    }

    // ── DB 재구성 생성자 ──────────────────────────────────────

    @Test
    void 생성자_DB재구성_필드가_그대로_유지된다() {
        Duty duty = new Duty(99L, ROOM_ID, WEEK, ZONE_ID, ASSIGNEE,
                             DutyStatus.DONE, OTHER, TODAY);

        assertEquals(99L, duty.getId());
        assertEquals(DutyStatus.DONE, duty.getStatus());
        assertEquals(OTHER, duty.getDoneById());
        assertEquals(TODAY, duty.getDoneDate());
        assertTrue(duty.isSubstitute());
    }
}
