package com.example.cleanmate.domain.duty.repository;

import com.example.cleanmate.domain.duty.model.Duty;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * 테스트·개발용 메모리 DutyRepository.
 * JDBC 저장소로 교체할 때 이 클래스만 바꾸면 된다.
 */
@Repository
public class InMemoryDutyRepository implements DutyRepository {

    private final Map<String, Duty> store = new ConcurrentHashMap<>();
    private final AtomicLong idSeq = new AtomicLong(1);

    private String key(Long roomId, LocalDate weekStart, Long zoneId) {
        return roomId + "_" + weekStart + "_" + zoneId;
    }

    @Override
    public Duty save(Duty duty) {
        if (duty.getId() == null) {
            Long newId = idSeq.getAndIncrement();
            duty = new Duty(newId, duty.getRoomId(), duty.getWeekStart(), duty.getZoneId(),
                            duty.getAssigneeId(), duty.getStatus(), duty.getDoneById(), duty.getDoneDate());
        }
        store.put(key(duty.getRoomId(), duty.getWeekStart(), duty.getZoneId()), duty);
        return duty;
    }

    @Override
    public Optional<Duty> findByRoomAndWeekAndZone(Long roomId, LocalDate weekStart, Long zoneId) {
        return Optional.ofNullable(store.get(key(roomId, weekStart, zoneId)));
    }

    @Override
    public List<Duty> findByRoomAndWeek(Long roomId, LocalDate weekStart) {
        return store.values().stream()
                .filter(d -> d.getRoomId().equals(roomId) && d.getWeekStart().equals(weekStart))
                .toList();
    }

    @Override
    public boolean existsByRoomAndWeek(Long roomId, LocalDate weekStart) {
        return store.values().stream()
                .anyMatch(d -> d.getRoomId().equals(roomId) && d.getWeekStart().equals(weekStart));
    }
}
