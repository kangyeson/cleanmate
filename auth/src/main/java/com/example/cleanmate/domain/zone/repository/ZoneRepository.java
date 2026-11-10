package com.example.cleanmate.domain.zone.repository;

import com.example.cleanmate.domain.zone.model.Zone;

import java.util.List;

public interface ZoneRepository {
    List<Zone> findAllByRoomIdOrderByZoneOrder(Long roomId);

    /** 구역 저장 → DB에서 생성된 id가 채워진 Zone 반환. */
    Zone save(Zone zone);
}
