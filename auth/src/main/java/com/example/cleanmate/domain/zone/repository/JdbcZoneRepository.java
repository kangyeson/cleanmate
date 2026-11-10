package com.example.cleanmate.domain.zone.repository;

import com.example.cleanmate.domain.zone.model.Zone;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

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
}
