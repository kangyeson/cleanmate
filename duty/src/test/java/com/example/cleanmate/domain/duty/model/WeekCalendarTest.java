package com.example.cleanmate.domain.duty.model;

import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.LocalDate;
import java.time.ZoneId;

import static org.junit.jupiter.api.Assertions.*;

class WeekCalendarTest {

    private static final ZoneId SEOUL = ZoneId.of("Asia/Seoul");

    // 고정 날짜: 2026-11-05 (목요일)
    private static final Clock FIXED_CLOCK =
            Clock.fixed(LocalDate.of(2026, 11, 5).atStartOfDay(SEOUL).toInstant(), SEOUL);

    private final WeekCalendar cal = new WeekCalendar(FIXED_CLOCK);

    @Test
    void today_고정날짜를_반환한다() {
        assertEquals(LocalDate.of(2026, 11, 5), cal.today());
    }

    @Test
    void mondayOf_목요일이면_같은주_월요일() {
        // 2026-11-05(목) → 2026-11-02(월)
        assertEquals(
            LocalDate.of(2026, 11, 2),
            cal.mondayOf(LocalDate.of(2026, 11, 5))
        );
    }

    @Test
    void mondayOf_월요일이면_그대로() {
        LocalDate monday = LocalDate.of(2026, 11, 2);
        assertEquals(monday, cal.mondayOf(monday));
    }

    @Test
    void mondayOf_일요일이면_같은주_월요일() {
        // 2026-11-08(일) → 2026-11-02(월)
        assertEquals(
            LocalDate.of(2026, 11, 2),
            cal.mondayOf(LocalDate.of(2026, 11, 8))
        );
    }

    @Test
    void currentMonday_오늘기준_월요일() {
        // today = 11/05(목) → 11/02(월)
        assertEquals(LocalDate.of(2026, 11, 2), cal.currentMonday());
    }

    @Test
    void weekIndex_같은주면_0() {
        LocalDate start = LocalDate.of(2026, 11, 2);
        assertEquals(0, cal.weekIndex(start, start));
    }

    @Test
    void weekIndex_다음주면_1() {
        LocalDate start = LocalDate.of(2026, 11, 2);
        LocalDate next  = LocalDate.of(2026, 11, 9);
        assertEquals(1, cal.weekIndex(start, next));
    }

    @Test
    void weekIndex_4주후면_4() {
        LocalDate start = LocalDate.of(2026, 11, 2);
        LocalDate later = LocalDate.of(2026, 11, 30);
        assertEquals(4, cal.weekIndex(start, later));
    }

    @Test
    void weekIndex_targetWeek이_이전이면_예외() {
        LocalDate start  = LocalDate.of(2026, 11, 9);
        LocalDate before = LocalDate.of(2026, 11, 2);
        assertThrows(IllegalArgumentException.class, () -> cal.weekIndex(start, before));
    }
}
