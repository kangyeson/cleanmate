package com.example.cleanmate.domain.duty.model;

import java.time.LocalDate;

/**
 * 한 주·한 구역의 청소 당번 기록.
 *
 * <p>상태 전이:
 * <pre>
 *   PENDING --markDone()--> DONE --cancel()--> PENDING
 *   PENDING --markMissed()--> MISSED
 * </pre>
 *
 * <p>DB 테이블 duties 의 한 행에 대응한다.
 * id 는 DB 저장 전에는 null 이다.
 */
public class Duty {

    private final Long id;          // DB PK. 신규 생성 시 null
    private final Long roomId;
    private final LocalDate weekStart;  // 해당 주 월요일
    private final Long zoneId;
    private final Long assigneeId;      // 배정된 담당자

    private DutyStatus status;
    private Long doneById;          // 실제로 청소한 사람. DONE 일 때만 non-null
    private LocalDate doneDate;     // 청소 완료 날짜. DONE 일 때만 non-null

    /** DB 조회로 재구성할 때 사용 */
    public Duty(Long id, Long roomId, LocalDate weekStart, Long zoneId, Long assigneeId,
                DutyStatus status, Long doneById, LocalDate doneDate) {
        this.id = id;
        this.roomId = roomId;
        this.weekStart = weekStart;
        this.zoneId = zoneId;
        this.assigneeId = assigneeId;
        this.status = status;
        this.doneById = doneById;
        this.doneDate = doneDate;
    }

    /** 새 배정 생성 (DB 저장 전, id = null, status = PENDING) */
    public static Duty newAssignment(Long roomId, LocalDate weekStart, Long zoneId, Long assigneeId) {
        return new Duty(null, roomId, weekStart, zoneId, assigneeId,
                        DutyStatus.PENDING, null, null);
    }

    // ── 상태 변경 ──────────────────────────────────────────────

    /**
     * 완료 처리. PENDING 상태일 때만 호출 가능.
     *
     * @param doneById  실제로 청소한 사람의 memberId
     * @param doneDate  청소 완료 날짜
     */
    public void markDone(Long doneById, LocalDate doneDate) {
        if (status != DutyStatus.PENDING) {
            throw new IllegalStateException(
                "완료 처리는 PENDING 상태에서만 가능합니다. 현재 상태: " + status);
        }
        this.status = DutyStatus.DONE;
        this.doneById = doneById;
        this.doneDate = doneDate;
    }

    /**
     * 완료 취소. DONE 상태일 때만 호출 가능.
     * 취소 후 PENDING 으로 돌아가며 doneById·doneDate 는 초기화된다.
     */
    public void cancel() {
        if (status != DutyStatus.DONE) {
            throw new IllegalStateException(
                "취소는 DONE 상태에서만 가능합니다. 현재 상태: " + status);
        }
        this.status = DutyStatus.PENDING;
        this.doneById = null;
        this.doneDate = null;
    }

    /**
     * 미완료 확정. PENDING 상태일 때만 호출 가능.
     * WeeklyJob 이 주 전환 시 지난 주 PENDING 을 일괄 확정할 때 사용한다.
     */
    public void markMissed() {
        if (status != DutyStatus.PENDING) {
            throw new IllegalStateException(
                "미완료 확정은 PENDING 상태에서만 가능합니다. 현재 상태: " + status);
        }
        this.status = DutyStatus.MISSED;
    }

    /** 보정: 완료로 처리 (지난 주 보정용, 현재 상태 무관) */
    public void correctAsDone(Long doneById, LocalDate doneDate) {
        if (doneById == null || doneDate == null) {
            throw new IllegalArgumentException("doneById와 doneDate는 null이어서는 안 됩니다.");
        }
        this.status = DutyStatus.DONE;
        this.doneById = doneById;
        this.doneDate = doneDate;
    }

    /** 보정: 미완료로 처리 (지난 주 보정용, 현재 상태 무관) */
    public void correctAsMissed() {
        this.status = DutyStatus.MISSED;
        this.doneById = null;
        this.doneDate = null;
    }

    // ── 도메인 판단 ────────────────────────────────────────────

    /**
     * 대신 청소 여부.
     * 완료되었고 실제 청소한 사람이 담당자와 다를 때 true.
     */
    public boolean isSubstitute() {
        return status == DutyStatus.DONE && !doneById.equals(assigneeId);
    }

    // ── Getters ───────────────────────────────────────────────

    public Long getId()            { return id; }
    public Long getRoomId()        { return roomId; }
    public LocalDate getWeekStart(){ return weekStart; }
    public Long getZoneId()        { return zoneId; }
    public Long getAssigneeId()    { return assigneeId; }
    public DutyStatus getStatus()  { return status; }
    public Long getDoneById()      { return doneById; }
    public LocalDate getDoneDate() { return doneDate; }
}
