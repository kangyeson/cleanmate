package com.example.cleanmate.domain.duty.service;

import com.example.cleanmate.domain.client.StubRosterClient;
import com.example.cleanmate.domain.duty.dto.WeekDutyResponse;
import com.example.cleanmate.domain.duty.model.DutyStatus;
import com.example.cleanmate.domain.duty.model.RotationPolicy;
import com.example.cleanmate.domain.duty.model.WeekCalendar;
import com.example.cleanmate.domain.duty.repository.InMemoryDutyRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.*;

class DutyServiceTest {

    // 고정 날짜: 2026-11-05(목) → currentMonday = 2026-11-02
    private static final LocalDate TODAY = LocalDate.of(2026, 11, 5);

    private DutyService service;

    @BeforeEach
    void setUp() {
        service = new DutyService(
                new InMemoryDutyRepository(),
                new StubRosterClient(),
                new WeekCalendar(TODAY),
                new RotationPolicy()
        );
    }

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

        // id가 같아야 함 (새로 생성되지 않음)
        assertEquals(first.duties().get(0).zoneId(), second.duties().get(0).zoneId());
        assertEquals(first.duties().size(), second.duties().size());
    }

    @Test
    void getWeekDuties_11월2주_순환_배정_확인() {
        // 11/2 주 = weekIndex 0
        // 샤워실(10L) → 민정(1L), 화장실(20L) → 지현(2L), 세면대(30L) → 서현(3L), 바닥(40L) → 예손(4L)
        WeekDutyResponse result = service.getWeekDuties();

        WeekDutyResponse.DutyItem shower = findByZone(result, "샤워실");
        assertEquals("민정",  shower.assigneeName());
        assertEquals(DutyStatus.PENDING, shower.status());

        WeekDutyResponse.DutyItem toilet = findByZone(result, "화장실");
        assertEquals("지현", toilet.assigneeName());

        WeekDutyResponse.DutyItem sink = findByZone(result, "세면대");
        assertEquals("서현", sink.assigneeName());

        WeekDutyResponse.DutyItem floor = findByZone(result, "바닥");
        assertEquals("예손", floor.assigneeName());
    }

    @Test
    void getWeekDuties_초기상태는_전부_PENDING() {
        WeekDutyResponse result = service.getWeekDuties();

        result.duties().forEach(d ->
                assertEquals(DutyStatus.PENDING, d.status())
        );
    }

    @Test
    void getWeekDuties_구역명과_담당자명이_채워진다() {
        WeekDutyResponse result = service.getWeekDuties();

        result.duties().forEach(d -> {
            assertNotNull(d.zoneName());
            assertNotNull(d.assigneeName());
            assertFalse(d.zoneName().isBlank());
            assertFalse(d.assigneeName().isBlank());
        });
    }

    private WeekDutyResponse.DutyItem findByZone(WeekDutyResponse response, String zoneName) {
        return response.duties().stream()
                .filter(d -> d.zoneName().equals(zoneName))
                .findFirst()
                .orElseThrow(() -> new AssertionError("구역 없음: " + zoneName));
    }
}
