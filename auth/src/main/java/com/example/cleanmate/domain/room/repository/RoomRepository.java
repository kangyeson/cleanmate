package com.example.cleanmate.domain.room.repository;

import com.example.cleanmate.domain.room.model.Room;

import java.util.Optional;

public interface RoomRepository {
    /** 진행 중인 방(status='ACTIVE')을 반환. 없으면 empty. */
    Optional<Room> findActive();

    /** 방 저장 → DB에서 생성된 id가 채워진 Room 반환. */
    Room save(Room room);
}
