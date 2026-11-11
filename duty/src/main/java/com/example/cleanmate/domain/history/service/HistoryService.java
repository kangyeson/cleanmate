package com.example.cleanmate.domain.history.service;

import com.example.cleanmate.domain.client.RosterClient;
import com.example.cleanmate.domain.client.RosterResponse;
import com.example.cleanmate.domain.duty.model.Duty;
import com.example.cleanmate.domain.duty.model.DutyStatus;
import com.example.cleanmate.domain.duty.repository.DutyRepository;
import com.example.cleanmate.domain.history.dto.HistoryResponse;
import com.example.cleanmate.global.exception.CleanmateException;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.stream.Collectors;

@Service
public class HistoryService {
    private final DutyRepository dutyRepository;
    private final RosterClient rosterClient;

    public HistoryService(DutyRepository dutyRepository, RosterClient rosterClient) {
        this.dutyRepository = dutyRepository;
        this.rosterClient = rosterClient;
    }

    public HistoryResponse getHistory(LocalDate weekStartFilter) {
        RosterResponse roster = rosterClient.getRoster(); // NoActiveRoomException 전파

        // zone·member 이름·순서 조회용 Map
        List<RosterResponse.ZoneInfo> zones = roster.zones();
        Map<Long, Integer> zoneOrder = new java.util.HashMap<>();
        for (int i = 0; i < zones.size(); i++) zoneOrder.put(zones.get(i).id(), i);
        Map<Long, String> zoneNames = zones.stream()
            .collect(Collectors.toMap(RosterResponse.ZoneInfo::id, RosterResponse.ZoneInfo::name));
        Map<Long, String> memberNames = roster.members().stream()
            .collect(Collectors.toMap(RosterResponse.MemberInfo::id, RosterResponse.MemberInfo::name));

        List<Duty> all = dutyRepository.findByRoom(roster.roomId());

        // weekStartFilter가 있으면 해당 주만
        if (weekStartFilter != null) {
            List<Duty> week = all.stream()
                .filter(d -> d.getWeekStart().equals(weekStartFilter))
                .toList();
            if (week.isEmpty()) throw new CleanmateException("해당 주의 기록이 없습니다.", 404);
            all = week;
        }

        // week_start로 묶고 최신 주부터 정렬
        Map<LocalDate, List<Duty>> grouped = all.stream()
            .collect(Collectors.groupingBy(Duty::getWeekStart));
        TreeMap<LocalDate, List<Duty>> sorted = new TreeMap<>(Comparator.reverseOrder());
        sorted.putAll(grouped);

        List<HistoryResponse.WeekEntry> weeks = sorted.entrySet().stream()
            .map(e -> new HistoryResponse.WeekEntry(
                e.getKey(),
                e.getValue().stream()
                    .sorted(Comparator.comparingInt(d -> zoneOrder.getOrDefault(d.getZoneId(), Integer.MAX_VALUE)))
                    .map(d -> toItem(d, zoneNames, memberNames))
                    .toList()
            ))
            .toList();

        return new HistoryResponse(weeks);
    }

    private HistoryResponse.DutyItem toItem(Duty d,
            Map<Long, String> zoneNames, Map<Long, String> memberNames) {
        return new HistoryResponse.DutyItem(
            d.getZoneId(), zoneNames.getOrDefault(d.getZoneId(), "?"),
            d.getAssigneeId(), memberNames.getOrDefault(d.getAssigneeId(), "?"),
            d.getStatus(),
            d.getDoneById(),
            d.getDoneById() != null ? memberNames.getOrDefault(d.getDoneById(), "?") : null,
            d.getDoneDate()
        );
    }
}
