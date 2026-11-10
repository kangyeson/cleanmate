package com.example.cleanmate.domain.duty.repository;

import com.example.cleanmate.domain.duty.model.Duty;
import com.example.cleanmate.domain.duty.model.DutyStatus;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * 테스트 전용 메모리 DutyRepository.
 * Spring Bean 등록 없음(@Repository 없음) — 테스트에서 직접 new 로 생성.
 */
public class InMemoryDutyRepository implements DutyRepository {

    private final Map<String, Duty> store = new ConcurrentHashMap<>();
    private final AtomicLong idSeq = new AtomicLong(1);

    private String key(Long roomId, LocalDate weekStart, Long zoneId) {
        return roomId + "_" + weekStart + "_" + zoneId;
    }

    private Duty copy(Duty d) {
        return new Duty(d.getId(), d.getRoomId(), d.getWeekStart(), d.getZoneId(),
                d.getAssigneeId(), d.getStatus(), d.getDoneById(), d.getDoneDate());
    }

    @Override
    public Duty save(Duty duty) {
        Long id = duty.getId() != null ? duty.getId() : idSeq.getAndIncrement();
        // store 용 복사본 — 외부에서 변경해도 store 가 오염되지 않음
        Duty storeEntry = new Duty(id, duty.getRoomId(), duty.getWeekStart(), duty.getZoneId(),
                duty.getAssigneeId(), duty.getStatus(), duty.getDoneById(), duty.getDoneDate());
        store.put(key(storeEntry.getRoomId(), storeEntry.getWeekStart(), storeEntry.getZoneId()), storeEntry);
        // 반환 용 별도 복사본 — 호출자가 변경해도 store 의 storeEntry 는 불변
        return copy(storeEntry);
    }

    /**
     * 조건부 갱신 — 현재 상태가 requiredCurrentStatus 일 때만 갱신.
     * synchronized 로 check-then-act 를 원자화.
     */
    @Override
    public synchronized boolean updateIfStatus(Duty duty, DutyStatus requiredCurrentStatus) {
        String k = key(duty.getRoomId(), duty.getWeekStart(), duty.getZoneId());
        Duty current = store.get(k);
        if (current == null || current.getStatus() != requiredCurrentStatus) return false;
        store.put(k, copy(duty));
        return true;
    }

    @Override
    public Optional<Duty> findByRoomAndWeekAndZone(Long roomId, LocalDate weekStart, Long zoneId) {
        // 방어적 복사: 꺼낸 객체를 바꿔도 store 의 내부 상태는 불변
        return Optional.ofNullable(store.get(key(roomId, weekStart, zoneId))).map(this::copy);
    }

    @Override
    public List<Duty> findByRoomAndWeek(Long roomId, LocalDate weekStart) {
        return store.values().stream()
                .filter(d -> d.getRoomId().equals(roomId) && d.getWeekStart().equals(weekStart))
                .map(this::copy)
                .toList();
    }

    @Override
    public boolean existsByRoomAndWeek(Long roomId, LocalDate weekStart) {
        return store.values().stream()
                .anyMatch(d -> d.getRoomId().equals(roomId) && d.getWeekStart().equals(weekStart));
    }
}
