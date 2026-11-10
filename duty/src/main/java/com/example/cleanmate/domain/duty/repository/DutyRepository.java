package com.example.cleanmate.domain.duty.repository;

import com.example.cleanmate.domain.duty.model.Duty;
import com.example.cleanmate.domain.duty.model.DutyStatus;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface DutyRepository {

    /**
     * id == null 이면 삽입, non-null 이면 상태 그대로 갱신 (조건 없음).
     * 동시성이 필요한 상태 전이는 {@link #updateIfStatus}를 사용한다.
     * 삽입 시 중복 키 충돌(동시 배정 경쟁)은 흡수하고 기존 행을 반환한다.
     */
    Duty save(Duty duty);

    /**
     * 조건부 상태 갱신 — 동시성 안전.
     * DB/store 에서 현재 status == requiredCurrentStatus 인 경우에만 갱신하고 true 를 반환한다.
     * status 가 이미 다른 값이면 false 를 반환한다(다른 요청이 먼저 완료한 것).
     */
    boolean updateIfStatus(Duty duty, DutyStatus requiredCurrentStatus);

    Optional<Duty> findByRoomAndWeekAndZone(Long roomId, LocalDate weekStart, Long zoneId);

    List<Duty> findByRoomAndWeek(Long roomId, LocalDate weekStart);

    boolean existsByRoomAndWeek(Long roomId, LocalDate weekStart);
}
