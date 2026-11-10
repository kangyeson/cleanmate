package com.example.cleanmate.domain.zone.repository;

import com.example.cleanmate.domain.zone.model.Zone;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * 단위 테스트 전용 메모리 ZoneRepository. Spring 빈으로 등록하지 않는다.
 */
public class InMemoryZoneRepository implements ZoneRepository {

    private final List<Zone> store = new ArrayList<>();
    private long nextId = 1L;

    @Override
    public List<Zone> findAllByRoomIdOrderByZoneOrder(Long roomId) {
        return store.stream()
                .filter(z -> z.getRoomId().equals(roomId) && z.isActive())
                .sorted(Comparator.comparingInt(Zone::getZoneOrder))
                .toList();
    }

    @Override
    public Zone save(Zone zone) {
        long id = nextId++;
        Zone saved = new Zone(id, zone.getRoomId(), zone.getName(), zone.getZoneOrder(), zone.isActive());
        store.add(saved);
        return saved;
    }

    public List<Zone> findAll() {
        return List.copyOf(store);
    }
}
