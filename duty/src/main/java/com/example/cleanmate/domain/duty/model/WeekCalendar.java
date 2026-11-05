package com.example.cleanmate.domain.duty.model;

import java.time.Clock;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;

/**
 * 주(week) 관련 날짜 계산.
 * 주는 월~일, 기준 타임존은 Asia/Seoul.
 *
 * Clock을 주입받으므로 테스트에서 Clock.fixed(...)로 날짜를 고정할 수 있고,
 * 프로덕션에서는 Clock.system(...)으로 항상 현재 시각을 반영한다.
 */
public class WeekCalendar {

    private final Clock clock;

    public WeekCalendar(Clock clock) {
        this.clock = clock;
    }

    /** 오늘 날짜 (호출 시점의 clock 기준) */
    public LocalDate today() {
        return LocalDate.now(clock);
    }

    /** 주어진 날짜가 속한 주의 월요일 */
    public LocalDate mondayOf(LocalDate date) {
        return date.with(DayOfWeek.MONDAY);
    }

    /** 이번 주 월요일 */
    public LocalDate currentMonday() {
        return mondayOf(today());
    }

    /**
     * startWeek 기준으로 targetWeek이 몇 번째 주인지 (0-based).
     *
     * @param startWeek 방 생성 시 시작 주의 월요일
     * @param targetWeek 계산할 주의 월요일
     * @return 0 이상의 정수 (startWeek == targetWeek 이면 0)
     * @throws IllegalArgumentException targetWeek 이 startWeek 보다 이전인 경우
     */
    public int weekIndex(LocalDate startWeek, LocalDate targetWeek) {
        long diff = ChronoUnit.WEEKS.between(startWeek, targetWeek);
        if (diff < 0) {
            throw new IllegalArgumentException(
                "targetWeek(%s)이 startWeek(%s)보다 이전입니다.".formatted(targetWeek, startWeek)
            );
        }
        return (int) diff;
    }
}
