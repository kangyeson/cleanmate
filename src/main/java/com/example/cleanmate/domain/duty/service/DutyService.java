package com.example.cleanmate.domain.duty.service;

import com.example.cleanmate.domain.client.RosterClient;
import com.example.cleanmate.domain.client.RosterResponse;
import com.example.cleanmate.domain.duty.dto.WeekDutyResponse;
import com.example.cleanmate.domain.duty.model.Duty;
import com.example.cleanmate.domain.duty.model.RotationPolicy;
import com.example.cleanmate.domain.duty.model.WeekCalendar;
import com.example.cleanmate.domain.duty.repository.DutyRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@Service
public class DutyService {

    private final DutyRepository dutyRepository;
    private final RosterClient rosterClient;
    private final WeekCalendar weekCalendar;
    private final RotationPolicy rotationPolicy;

    public DutyService(DutyRepository dutyRepository, RosterClient rosterClient,
                       WeekCalendar weekCalendar, RotationPolicy rotationPolicy) {
        this.dutyRepository = dutyRepository;
        this.rosterClient = rosterClient;
        this.weekCalendar = weekCalendar;
        this.rotationPolicy = rotationPolicy;
    }

    /**
     * 이번 주 현황을 반환한다.
     * 아직 배정이 없으면 먼저 생성한다.
     */
    public WeekDutyResponse getWeekDuties() {
        RosterResponse roster = rosterClient.getRoster();
        LocalDate weekStart = weekCalendar.currentMonday();
        assignWeekIfAbsent(weekStart, roster);
        List<Duty> duties = dutyRepository.findByRoomAndWeek(roster.roomId(), weekStart);
        return toResponse(weekStart, duties, roster);
    }

    /**
     * 해당 주 배정이 없을 때만 생성한다 (멱등).
     * RotationPolicy 공식: (i + weekIndex) % 인원수
     */
    private void assignWeekIfAbsent(LocalDate weekStart, RosterResponse roster) {
        if (dutyRepository.existsByRoomAndWeek(roster.roomId(), weekStart)) {
            return;
        }
        int weekIndex = weekCalendar.weekIndex(roster.startWeek(), weekStart);
        Map<Long, Long> assignments = rotationPolicy.assign(roster.zoneIds(), roster.memberIds(), weekIndex);
        for (Map.Entry<Long, Long> e : assignments.entrySet()) {
            dutyRepository.save(Duty.newAssignment(roster.roomId(), weekStart, e.getKey(), e.getValue()));
        }
    }

    private WeekDutyResponse toResponse(LocalDate weekStart, List<Duty> duties, RosterResponse roster) {
        List<WeekDutyResponse.DutyItem> items = duties.stream()
                .map(d -> new WeekDutyResponse.DutyItem(
                        d.getZoneId(),
                        roster.zoneName(d.getZoneId()),
                        d.getAssigneeId(),
                        roster.memberName(d.getAssigneeId()),
                        d.getStatus(),
                        d.getDoneById(),
                        d.getDoneById() != null ? roster.memberName(d.getDoneById()) : null,
                        d.getDoneDate()
                ))
                .toList();
        return new WeekDutyResponse(weekStart, items);
    }
}
