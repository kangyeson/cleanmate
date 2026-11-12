package com.example.cleanmate.domain.stats.service;

import com.example.cleanmate.domain.client.RosterClient;
import com.example.cleanmate.domain.client.RosterResponse;
import com.example.cleanmate.domain.duty.model.Duty;
import com.example.cleanmate.domain.duty.model.DutyStatus;
import com.example.cleanmate.domain.duty.model.WeekCalendar;
import com.example.cleanmate.domain.duty.repository.DutyRepository;
import com.example.cleanmate.domain.stats.dto.StatsResponse;
import com.example.cleanmate.domain.stats.dto.StatsResponse.MemberStat;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class StatsService {

    private final DutyRepository dutyRepository;
    private final RosterClient rosterClient;
    private final WeekCalendar weekCalendar;

    public StatsService(DutyRepository dutyRepository,
                        RosterClient rosterClient,
                        WeekCalendar weekCalendar) {
        this.dutyRepository = dutyRepository;
        this.rosterClient = rosterClient;
        this.weekCalendar = weekCalendar;
    }

    /**
     * 방 전체 멤버의 청소 통계 조회.
     * 확정된 주(week_start < 이번 주 월요일)만 집계해 담당·완료·이행률 기준을 통일한다.
     * 활성 방이 없으면 NoActiveRoomException (404) 전파.
     */
    public StatsResponse getStats() {
        RosterResponse roster = rosterClient.getRoster();
        LocalDate currentMonday = weekCalendar.currentMonday();

        // 확정된 주만 추린다 (이번 주 제외)
        List<Duty> confirmed = dutyRepository.findByRoom(roster.roomId()).stream()
                .filter(d -> d.getWeekStart().isBefore(currentMonday))
                .toList();

        int confirmedWeeks = (int) confirmed.stream()
                .map(Duty::getWeekStart)
                .distinct()
                .count();

        // 사람별 집계 객체 초기화 (활성·비활성 모두)
        Map<Long, Counter> counters = new HashMap<>();
        for (RosterResponse.MemberInfo m : roster.members()) {
            counters.put(m.id(), new Counter());
        }

        // Duty 목록을 한 번만 순회하며 집계
        for (Duty d : confirmed) {
            Counter assigneeCounter = counters.get(d.getAssigneeId());
            if (assigneeCounter != null) {
                assigneeCounter.assigned++;
                if (d.getStatus() == DutyStatus.DONE
                        && d.getAssigneeId().equals(d.getDoneById())) {
                    assigneeCounter.selfDone++;
                } else if (d.getStatus() != DutyStatus.DONE) {
                    // MISSED 또는 확정된 주의 PENDING → 미완료
                    assigneeCounter.missed++;
                }
            }

            // 대신 청소: doneBy ≠ assignee
            if (d.getStatus() == DutyStatus.DONE && d.getDoneById() != null
                    && !d.getAssigneeId().equals(d.getDoneById())) {
                Counter doneByCounter = counters.get(d.getDoneById());
                if (doneByCounter != null) {
                    doneByCounter.substitute++;
                }
            }
        }

        // 결과 조립 — 활성 멤버 or 기록이 있는 비활성 멤버만 포함
        List<MemberStat> stats = roster.members().stream()
                .filter(m -> {
                    Counter c = counters.get(m.id());
                    return m.active() || (c != null && (c.assigned > 0 || c.substitute > 0));
                })
                .map(m -> {
                    Counter c = counters.get(m.id());
                    String rate = c.assigned == 0
                            ? null
                            : (c.selfDone * 100 / c.assigned) + "%";
                    return new MemberStat(m.id(), m.name(), m.active(),
                            c.assigned, c.selfDone, c.missed, c.substitute, rate);
                })
                .toList();

        return new StatsResponse(confirmedWeeks, stats);
    }

    /** 멤버 1인의 집계 중간값 */
    private static class Counter {
        int assigned, selfDone, missed, substitute;
    }
}
