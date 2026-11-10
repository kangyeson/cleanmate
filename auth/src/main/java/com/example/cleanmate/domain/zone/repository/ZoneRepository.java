package com.example.cleanmate.domain.zone.repository;

import com.example.cleanmate.domain.zone.model.Zone;

import java.util.List;

public interface ZoneRepository {
    List<Zone> findAllByRoomIdOrderByZoneOrder(Long roomId);
}
