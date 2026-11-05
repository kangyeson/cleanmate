package com.example.cleanmate.domain.duty.repository;

import com.example.cleanmate.domain.duty.model.Duty;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface DutyRepository {

    /** 저장(id==null 이면 insert, non-null 이면 update). 저장된 객체를 반환한다. */
    Duty save(Duty duty);

    Optional<Duty> findByRoomAndWeekAndZone(Long roomId, LocalDate weekStart, Long zoneId);

    List<Duty> findByRoomAndWeek(Long roomId, LocalDate weekStart);

    boolean existsByRoomAndWeek(Long roomId, LocalDate weekStart);
}
