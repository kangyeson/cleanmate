package com.example.cleanmate.domain.zone.repository;

import com.example.cleanmate.domain.zone.model.Zone;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

/**
 * 단위 테스트 전용 메모리 ZoneRepository. Spring 빈으로 등록하지 않는다.
 */
public class InMemoryZoneRepository implements ZoneRepository {

    private final List<Zone> store = new ArrayList<>();
    private long nextId = 1L;

    @Override
    public List<Zone> findAllByRoomId(Long roomId) {
        return store.stream()
                .filter(z -> z.getRoomId().equals(roomId))
                .sorted(Comparator.comparingInt(Zone::getZoneOrder))
                .toList();
    }

    @Override
    public Optional<Zone> findById(Long id) {
        return store.stream().filter(z -> z.getId() != null && z.getId().equals(id)).findFirst();
    }

    @Override
    public Zone save(Zone zone) {
        long id = nextId++;
        Zone saved = new Zone(id, zone.getRoomId(), zone.getName(), zone.getZoneOrder(), zone.isActive());
        store.add(saved);
        return saved;
    }

    @Override
    public void deactivate(Long zoneId) {
        store.replaceAll(z -> z.getId() != null && z.getId().equals(zoneId)
            ? new Zone(z.getId(), z.getRoomId(), z.getName(), z.getZoneOrder(), false)
            : z);
    }

    @Override
    public void updateName(Long zoneId, String name) {
        store.replaceAll(z -> z.getId() != null && z.getId().equals(zoneId)
            ? new Zone(z.getId(), z.getRoomId(), name, z.getZoneOrder(), z.isActive())
            : z);
    }

    @Override
    public void updateZoneOrders(Long roomId, List<Long> zoneIdsInNewOrder) {
        for (int i = 0; i < zoneIdsInNewOrder.size(); i++) {
            final Long zoneId = zoneIdsInNewOrder.get(i);
            final int newOrder = i + 1;
            store.replaceAll(z -> z.getId() != null && z.getId().equals(zoneId) && z.getRoomId().equals(roomId)
                ? new Zone(z.getId(), z.getRoomId(), z.getName(), newOrder, z.isActive())
                : z);
        }
    }

    public List<Zone> findAll() {
        return List.copyOf(store);
    }
}
