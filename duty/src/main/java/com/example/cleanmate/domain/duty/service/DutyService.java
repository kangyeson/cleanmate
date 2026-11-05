package com.example.cleanmate.domain.duty.service;

import com.example.cleanmate.domain.client.RosterClient;
import com.example.cleanmate.domain.client.RosterResponse;
import com.example.cleanmate.domain.duty.dto.WeekDutyResponse;
import com.example.cleanmate.domain.duty.exception.AlreadyDoneException;
import com.example.cleanmate.domain.duty.exception.DutyNotFoundException;
import com.example.cleanmate.domain.duty.exception.NotAssigneeException;
import com.example.cleanmate.domain.duty.exception.NotRecordOwnerException;
import com.example.cleanmate.domain.duty.model.Duty;
import com.example.cleanmate.domain.duty.model.DutyStatus;
import com.example.cleanmate.domain.duty.model.RotationPolicy;
import com.example.cleanmate.domain.duty.model.WeekCalendar;
import com.example.cleanmate.domain.duty.repository.DutyRepository;
import com.example.cleanmate.global.event.DutyEvent;
import com.example.cleanmate.global.event.EventPublisher;
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
    private final EventPublisher eventPublisher;

    public DutyService(DutyRepository dutyRepository, RosterClient rosterClient,
                       WeekCalendar weekCalendar, RotationPolicy rotationPolicy,
                       EventPublisher eventPublisher) {
        this.dutyRepository = dutyRepository;
        this.rosterClient = rosterClient;
        this.weekCalendar = weekCalendar;
        this.rotationPolicy = rotationPolicy;
        this.eventPublisher = eventPublisher;
    }

    // ── 조회 ──────────────────────────────────────────────────

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
     * 로그인한 사용자의 이번 주 담당 구역 목록을 반환한다.
     * 한 명이 여러 구역을 담당할 수 있으므로 List로 반환. 담당 구역 없으면 빈 리스트.
     */
    public List<WeekDutyResponse.DutyItem> getMyDuty(Long memberId) {
        RosterResponse roster = rosterClient.getRoster();
        LocalDate weekStart = weekCalendar.currentMonday();
        assignWeekIfAbsent(weekStart, roster);
        return dutyRepository.findByRoomAndWeek(roster.roomId(), weekStart).stream()
                .filter(d -> d.getAssigneeId().equals(memberId))
                .map(d -> toItem(d, roster))
                .toList();
    }

    // ── 기록 ──────────────────────────────────────────────────

    /**
     * 본인 담당 구역 완료 처리.
     * memberId == assigneeId 인 경우에만 허용.
     */
    public void markDone(Long zoneId, Long memberId) {
        RosterResponse roster = rosterClient.getRoster();
        LocalDate weekStart = weekCalendar.currentMonday();
        Duty duty = getDutyOrThrow(roster.roomId(), weekStart, zoneId);

        if (!duty.getAssigneeId().equals(memberId)) {
            throw new NotAssigneeException();
        }
        if (duty.getStatus() != DutyStatus.PENDING) {
            throw new AlreadyDoneException();
        }
        duty.markDone(memberId, weekCalendar.today());
        dutyRepository.save(duty);
        eventPublisher.publish(new DutyEvent(roster.roomId(), zoneId, duty.getAssigneeId(), memberId, false));
    }

    /**
     * 대신 청소.
     * 본인 담당이 아닌 PENDING 구역에만 허용.
     */
    public void markSubstitute(Long zoneId, Long memberId) {
        RosterResponse roster = rosterClient.getRoster();
        LocalDate weekStart = weekCalendar.currentMonday();
        Duty duty = getDutyOrThrow(roster.roomId(), weekStart, zoneId);

        if (duty.getAssigneeId().equals(memberId)) {
            throw new NotAssigneeException(); // 본인 담당은 /done 사용
        }
        if (duty.getStatus() != DutyStatus.PENDING) {
            throw new AlreadyDoneException();
        }
        duty.markDone(memberId, weekCalendar.today());
        dutyRepository.save(duty);
        eventPublisher.publish(new DutyEvent(roster.roomId(), zoneId, duty.getAssigneeId(), memberId, true));
    }

    /**
     * 완료 기록 취소.
     * 본인이 기록한 항목(doneById == memberId)만 취소 가능.
     */
    public void cancelRecord(Long zoneId, Long memberId) {
        RosterResponse roster = rosterClient.getRoster();
        LocalDate weekStart = weekCalendar.currentMonday();
        Duty duty = getDutyOrThrow(roster.roomId(), weekStart, zoneId);

        if (duty.getStatus() != DutyStatus.DONE) {
            throw new AlreadyDoneException(); // PENDING/MISSED 상태는 취소 불가
        }
        if (!duty.getDoneById().equals(memberId)) {
            throw new NotRecordOwnerException();
        }
        duty.cancel();
        dutyRepository.save(duty);
    }

    // ── 내부 헬퍼 ─────────────────────────────────────────────

    private Duty getDutyOrThrow(Long roomId, LocalDate weekStart, Long zoneId) {
        return dutyRepository.findByRoomAndWeekAndZone(roomId, weekStart, zoneId)
                .orElseThrow(DutyNotFoundException::new);
    }

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

    private WeekDutyResponse.DutyItem toItem(Duty d, RosterResponse roster) {
        return new WeekDutyResponse.DutyItem(
                d.getZoneId(),
                roster.zoneName(d.getZoneId()),
                d.getAssigneeId(),
                roster.memberName(d.getAssigneeId()),
                d.getStatus(),
                d.getDoneById(),
                d.getDoneById() != null ? roster.memberName(d.getDoneById()) : null,
                d.getDoneDate()
        );
    }

    private WeekDutyResponse toResponse(LocalDate weekStart, List<Duty> duties, RosterResponse roster) {
        List<WeekDutyResponse.DutyItem> items = duties.stream()
                .map(d -> toItem(d, roster))
                .toList();
        return new WeekDutyResponse(weekStart, items);
    }
}
