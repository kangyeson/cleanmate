package com.example.cleanmate.domain.duty.model;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;

/**
 * 주(week) 관련 날짜 계산.
 * 주는 월~일, 기준 타임존은 Asia/Seoul.
 *
 * 테스트 시 WeekCalendar(fixedDate)로 오늘 날짜를 고정할 수 있다.
 */
public class WeekCalendar {

    private static final ZoneId SEOUL = ZoneId.of("Asia/Seoul");

    private final LocalDate today;

    /** 프로덕션용 — 현재 시각 기준 오늘(Seoul) */
    public WeekCalendar() {
        this.today = LocalDate.now(SEOUL);
    }

    /** 테스트용 — 오늘 날짜 고정 */
    public WeekCalendar(LocalDate today) {
        this.today = today;
    }

    /** 오늘 날짜 */
    public LocalDate today() {
        return today;
    }

    /** 주어진 날짜가 속한 주의 월요일 */
    public LocalDate mondayOf(LocalDate date) {
        return date.with(DayOfWeek.MONDAY);
    }

    /** 이번 주 월요일 */
    public LocalDate currentMonday() {
        return mondayOf(today);
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
