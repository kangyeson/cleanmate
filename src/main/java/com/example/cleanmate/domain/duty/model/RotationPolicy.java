package com.example.cleanmate.domain.duty.model;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 청소 당번 순환 배정 정책.
 *
 * 규칙: n번째 주의 i번째 구역 담당자 = members.get((i + n) % members.size())
 * - n: weekIndex (0-based, 방 시작 주가 0)
 * - i: 구역 순서 (0-based, zone_order 기준)
 */
public class RotationPolicy {

    /**
     * 한 주의 구역별 담당자를 계산한다.
     *
     * @param zoneIds   구역 ID 목록 (zone_order 오름차순)
     * @param memberIds 룸메이트 ID 목록 (turn_order 오름차순)
     * @param weekIndex 방 시작 주 기준 주 번호 (0-based)
     * @return zoneId → assignee memberId 매핑 (입력 순서 유지)
     */
    public Map<Long, Long> assign(List<Long> zoneIds, List<Long> memberIds, int weekIndex) {
        if (zoneIds.isEmpty()) {
            throw new IllegalArgumentException("구역이 1개 이상이어야 합니다.");
        }
        if (memberIds.isEmpty()) {
            throw new IllegalArgumentException("룸메이트가 1명 이상이어야 합니다.");
        }
        if (weekIndex < 0) {
            throw new IllegalArgumentException("weekIndex는 0 이상이어야 합니다.");
        }

        int n = memberIds.size();
        Map<Long, Long> result = new LinkedHashMap<>();
        for (int i = 0; i < zoneIds.size(); i++) {
            Long assignee = memberIds.get((i + weekIndex) % n);
            result.put(zoneIds.get(i), assignee);
        }
        return result;
    }
}
