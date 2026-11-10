package com.example.cleanmate.domain.room.repository;

import com.example.cleanmate.domain.room.model.Room;

import java.util.Optional;

public interface RoomRepository {
    /** 진행 중인 방(status='ACTIVE')을 반환. 없으면 empty. */
    Optional<Room> findActive();
}
