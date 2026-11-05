package com.example.cleanmate.domain.duty.model;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class RotationPolicyTest {

    private final RotationPolicy policy = new RotationPolicy();

    /*
     * 예시 데이터 (인수인계 문서 3장)
     * 순번: 민정(1L) → 지현(2L) → 서현(3L) → 예손(4L)
     * 구역: 샤워실(10L) → 화장실(20L) → 세면대(30L) → 바닥(40L)
     * 시작 주(11/2, weekIndex=0): 샤워실-민정, 화장실-지현, 세면대-서현, 바닥-예손
     * 다음 주(11/9, weekIndex=1): 샤워실-지현, 화장실-서현, 세면대-예손, 바닥-민정
     */
    private final List<Long> zones   = List.of(10L, 20L, 30L, 40L);
    private final List<Long> members = List.of(1L, 2L, 3L, 4L);

    @Test
    void assign_weekIndex0_시작주_배정() {
        Map<Long, Long> result = policy.assign(zones, members, 0);

        assertEquals(1L, result.get(10L)); // 샤워실 → 민정
        assertEquals(2L, result.get(20L)); // 화장실 → 지현
        assertEquals(3L, result.get(30L)); // 세면대 → 서현
        assertEquals(4L, result.get(40L)); // 바닥   → 예손
    }

    @Test
    void assign_weekIndex1_다음주_배정() {
        Map<Long, Long> result = policy.assign(zones, members, 1);

        assertEquals(2L, result.get(10L)); // 샤워실 → 지현
        assertEquals(3L, result.get(20L)); // 화장실 → 서현
        assertEquals(4L, result.get(30L)); // 세면대 → 예손
        assertEquals(1L, result.get(40L)); // 바닥   → 민정
    }

    @Test
    void assign_weekIndex4_4번_순환하면_처음과_동일() {
        // 4주 = 인원수와 동일 → weekIndex=0 과 같아야 함
        Map<Long, Long> week0 = policy.assign(zones, members, 0);
        Map<Long, Long> week4 = policy.assign(zones, members, 4);
        assertEquals(week0, week4);
    }

    @Test
    void assign_구역_3개_룸메이트_4명() {
        // 구역 수 ≠ 인원 수인 경우
        List<Long> threeZones = List.of(10L, 20L, 30L);

        Map<Long, Long> result = policy.assign(threeZones, members, 0);
        assertEquals(1L, result.get(10L)); // (0+0)%4=0 → 민정
        assertEquals(2L, result.get(20L)); // (1+0)%4=1 → 지현
        assertEquals(3L, result.get(30L)); // (2+0)%4=2 → 서현

        Map<Long, Long> result1 = policy.assign(threeZones, members, 1);
        assertEquals(2L, result1.get(10L)); // (0+1)%4=1 → 지현
        assertEquals(3L, result1.get(20L)); // (1+1)%4=2 → 서현
        assertEquals(4L, result1.get(30L)); // (2+1)%4=3 → 예손
    }

    @Test
    void assign_구역없으면_예외() {
        assertThrows(IllegalArgumentException.class,
            () -> policy.assign(List.of(), members, 0));
    }

    @Test
    void assign_룸메이트없으면_예외() {
        assertThrows(IllegalArgumentException.class,
            () -> policy.assign(zones, List.of(), 0));
    }

    @Test
    void assign_weekIndex음수면_예외() {
        assertThrows(IllegalArgumentException.class,
            () -> policy.assign(zones, members, -1));
    }
}
