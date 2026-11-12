package com.example.cleanmate.domain.zone.repository;

import com.example.cleanmate.domain.zone.model.Zone;

import java.util.List;
import java.util.Optional;

public interface ZoneRepository {
    /** 해당 방의 모든 구역(비활성 포함), zone_order 오름차순. */
    List<Zone> findAllByRoomId(Long roomId);

    Optional<Zone> findById(Long id);

    /** 구역 저장 → DB에서 생성된 id가 채워진 Zone 반환. */
    Zone save(Zone zone);

    /** 구역 비활성화 (active = FALSE) */
    void deactivate(Long zoneId);

    /** 이름 변경 */
    void updateName(Long zoneId, String name);

    /** zone_order를 리스트 순서(1부터)로 일괄 갱신 */
    void updateZoneOrders(Long roomId, List<Long> zoneIdsInNewOrder);
}
