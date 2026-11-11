package com.example.cleanmate.domain.duty.repository;

import com.example.cleanmate.domain.duty.model.Duty;
import com.example.cleanmate.domain.duty.model.DutyStatus;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;

import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.Statement;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public class JdbcDutyRepository implements DutyRepository {

    private final JdbcTemplate jdbc;

    public JdbcDutyRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    private final RowMapper<Duty> rowMapper = (rs, rowNum) -> new Duty(
            rs.getLong("id"),
            rs.getLong("room_id"),
            rs.getDate("week_start").toLocalDate(),
            rs.getLong("zone_id"),
            rs.getLong("assignee_id"),
            DutyStatus.valueOf(rs.getString("status")),
            rs.getObject("done_by_id") != null ? rs.getLong("done_by_id") : null,
            rs.getDate("done_date") != null ? rs.getDate("done_date").toLocalDate() : null
    );

    @Override
    public Duty save(Duty duty) {
        if (duty.getId() == null) {
            return insert(duty);
        } else {
            update(duty);
            return duty;
        }
    }

    private Duty insert(Duty duty) {
        KeyHolder keyHolder = new GeneratedKeyHolder();
        try {
            jdbc.update(con -> {
                PreparedStatement ps = con.prepareStatement(
                        "INSERT INTO duties (room_id, week_start, zone_id, assignee_id, status, done_by_id, done_date) " +
                        "VALUES (?, ?, ?, ?, ?, ?, ?)",
                        Statement.RETURN_GENERATED_KEYS
                );
                ps.setLong(1, duty.getRoomId());
                ps.setDate(2, Date.valueOf(duty.getWeekStart()));
                ps.setLong(3, duty.getZoneId());
                ps.setLong(4, duty.getAssigneeId());
                ps.setString(5, duty.getStatus().name());
                if (duty.getDoneById() != null) ps.setLong(6, duty.getDoneById());
                else ps.setNull(6, java.sql.Types.BIGINT);
                if (duty.getDoneDate() != null) ps.setDate(7, Date.valueOf(duty.getDoneDate()));
                else ps.setNull(7, java.sql.Types.DATE);
                return ps;
            }, keyHolder);

            long newId = keyHolder.getKey().longValue();
            return new Duty(newId, duty.getRoomId(), duty.getWeekStart(), duty.getZoneId(),
                    duty.getAssigneeId(), duty.getStatus(), duty.getDoneById(), duty.getDoneDate());

        } catch (DuplicateKeyException e) {
            // 동시 요청이 같은 주·구역을 먼저 삽입 — 이미 존재하는 행을 그대로 반환
            return findByRoomAndWeekAndZone(duty.getRoomId(), duty.getWeekStart(), duty.getZoneId())
                    .orElseThrow(() -> e);
        }
    }

    private void update(Duty duty) {
        jdbc.update(
                "UPDATE duties SET status = ?, done_by_id = ?, done_date = ? WHERE id = ?",
                duty.getStatus().name(),
                duty.getDoneById(),
                duty.getDoneDate() != null ? Date.valueOf(duty.getDoneDate()) : null,
                duty.getId()
        );
    }

    /**
     * 조건부 갱신 — AND status = ? 절로 동시 완료를 막는다.
     * 바뀐 행이 없으면(다른 요청이 먼저 완료) false 반환.
     */
    @Override
    public boolean updateIfStatus(Duty duty, DutyStatus requiredCurrentStatus) {
        int updated = jdbc.update(
                "UPDATE duties SET status = ?, done_by_id = ?, done_date = ? WHERE id = ? AND status = ?",
                duty.getStatus().name(),
                duty.getDoneById(),
                duty.getDoneDate() != null ? Date.valueOf(duty.getDoneDate()) : null,
                duty.getId(),
                requiredCurrentStatus.name()
        );
        return updated > 0;
    }

    @Override
    public Optional<Duty> findByRoomAndWeekAndZone(Long roomId, LocalDate weekStart, Long zoneId) {
        List<Duty> result = jdbc.query(
                "SELECT * FROM duties WHERE room_id = ? AND week_start = ? AND zone_id = ?",
                rowMapper, roomId, Date.valueOf(weekStart), zoneId
        );
        return result.stream().findFirst();
    }

    @Override
    public List<Duty> findByRoomAndWeek(Long roomId, LocalDate weekStart) {
        return jdbc.query(
                "SELECT * FROM duties WHERE room_id = ? AND week_start = ?",
                rowMapper, roomId, Date.valueOf(weekStart)
        );
    }

    @Override
    public boolean existsByRoomAndWeek(Long roomId, LocalDate weekStart) {
        Integer count = jdbc.queryForObject(
                "SELECT COUNT(*) FROM duties WHERE room_id = ? AND week_start = ?",
                Integer.class, roomId, Date.valueOf(weekStart)
        );
        return count != null && count > 0;
    }

    @Override
    public List<Duty> findByRoom(Long roomId) {
        return jdbc.query("SELECT * FROM duties WHERE room_id = ?", rowMapper, roomId);
    }

    @Override
    public int markMissedBefore(Long roomId, LocalDate before) {
        return jdbc.update(
            "UPDATE duties SET status='MISSED' WHERE room_id=? AND week_start < ? AND status='PENDING'",
            roomId, Date.valueOf(before)
        );
    }
}
