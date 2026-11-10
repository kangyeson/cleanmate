package com.example.cleanmate.domain.room.model;

import java.time.LocalDate;

/**
 * 방 도메인 모델. 순수 Java — JPA 없음.
 */
public class Room {

    private final Long id;
    private final String name;
    private final LocalDate startWeek;  // 방 시작 주의 월요일 (주 번호 0)
    private final RoomStatus status;

    public Room(Long id, String name, LocalDate startWeek, RoomStatus status) {
        this.id = id;
        this.name = name;
        this.startWeek = startWeek;
        this.status = status;
    }

    public Long getId()             { return id; }
    public String getName()         { return name; }
    public LocalDate getStartWeek() { return startWeek; }
    public RoomStatus getStatus()   { return status; }
}
