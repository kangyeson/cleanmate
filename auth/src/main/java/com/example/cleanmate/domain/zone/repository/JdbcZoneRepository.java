package com.example.cleanmate.domain.zone.repository;

import com.example.cleanmate.domain.zone.model.Zone;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;

import java.sql.PreparedStatement;
import java.sql.Statement;
import java.util.List;

@Repository
public class JdbcZoneRepository implements ZoneRepository {

    private final JdbcTemplate jdbc;

    public JdbcZoneRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    private final RowMapper<Zone> rowMapper = (rs, rowNum) -> new Zone(
            rs.getLong("id"),
            rs.getLong("room_id"),
            rs.getString("name"),
            rs.getInt("zone_order"),
            rs.getBoolean("active")
    );

    @Override
    public List<Zone> findAllByRoomIdOrderByZoneOrder(Long roomId) {
        return jdbc.query(
                """
                SELECT * FROM zones
                WHERE room_id = ? AND active = TRUE
                ORDER BY zone_order
                """,
                rowMapper, roomId
        );
    }

    @Override
    public Zone save(Zone zone) {
        KeyHolder keyHolder = new GeneratedKeyHolder();
        jdbc.update(con -> {
            PreparedStatement ps = con.prepareStatement(
                    "INSERT INTO zones (room_id, name, zone_order, active) VALUES (?, ?, ?, ?)",
                    Statement.RETURN_GENERATED_KEYS
            );
            ps.setLong(1, zone.getRoomId());
            ps.setString(2, zone.getName());
            ps.setInt(3, zone.getZoneOrder());
            ps.setBoolean(4, zone.isActive());
            return ps;
        }, keyHolder);
        long newId = keyHolder.getKey().longValue();
        return new Zone(newId, zone.getRoomId(), zone.getName(), zone.getZoneOrder(), zone.isActive());
    }
}
