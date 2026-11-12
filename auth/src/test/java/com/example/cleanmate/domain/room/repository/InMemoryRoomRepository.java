package com.example.cleanmate.domain.room.repository;

import com.example.cleanmate.domain.room.exception.RoomAlreadyActiveException;
import com.example.cleanmate.domain.room.model.Room;
import com.example.cleanmate.domain.room.model.RoomStatus;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * 단위 테스트 전용 메모리 RoomRepository. Spring 빈으로 등록하지 않는다.
 */
public class InMemoryRoomRepository implements RoomRepository {

    private final List<Room> store = new ArrayList<>();
    private long nextId = 1L;

    /** 이미 진행 중인 방이 있는 상태로 초기화. */
    public void seedActiveRoom(Room room) {
        store.add(room);
    }

    @Override
    public Optional<Room> findActive() {
        return store.stream()
                .filter(r -> r.getStatus() == RoomStatus.ACTIVE)
                .findFirst();
    }

    @Override
    public Room save(Room room) {
        // uq_rooms_one_active 제약 시뮬레이션
        if (room.getStatus() == RoomStatus.ACTIVE && findActive().isPresent()) {
            throw new RoomAlreadyActiveException();
        }
        long id = nextId++;
        Room saved = new Room(id, room.getName(), room.getStartWeek(), room.getStatus());
        store.add(saved);
        return saved;
    }

    @Override
    public void close(Long roomId, LocalDate endDate) {
        store.replaceAll(r -> r.getId() != null && r.getId().equals(roomId)
            ? new Room(r.getId(), r.getName(), r.getStartWeek(), RoomStatus.CLOSED)
            : r);
    }

    public List<Room> findAll() {
        return List.copyOf(store);
    }
}
