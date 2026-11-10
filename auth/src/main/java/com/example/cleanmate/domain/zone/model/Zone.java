package com.example.cleanmate.domain.zone.model;

/**
 * 청소 구역 도메인 모델. 순수 Java — JPA 없음.
 */
public class Zone {

    private final Long id;
    private final Long roomId;
    private final String name;
    private final int zoneOrder;
    private final boolean active;

    public Zone(Long id, Long roomId, String name, int zoneOrder, boolean active) {
        this.id = id;
        this.roomId = roomId;
        this.name = name;
        this.zoneOrder = zoneOrder;
        this.active = active;
    }

    public Long getId()       { return id; }
    public Long getRoomId()   { return roomId; }
    public String getName()   { return name; }
    public int getZoneOrder() { return zoneOrder; }
    public boolean isActive() { return active; }
}
