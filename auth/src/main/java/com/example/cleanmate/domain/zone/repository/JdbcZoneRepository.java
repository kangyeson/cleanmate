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
import java.util.Optional;

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
    public List<Zone> findAllByRoomId(Long roomId) {
        return jdbc.query(
                """
                SELECT * FROM zones
                WHERE room_id = ?
                ORDER BY zone_order
                """,
                rowMapper, roomId
        );
    }

    @Override
    public Optional<Zone> findById(Long id) {
        List<Zone> result = jdbc.query(
                "SELECT * FROM zones WHERE id = ?",
                rowMapper, id
        );
        return result.stream().findFirst();
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

    @Override
    public void deactivate(Long zoneId) {
        jdbc.update("UPDATE zones SET active=FALSE WHERE id=?", zoneId);
    }

    @Override
    public void updateName(Long zoneId, String name) {
        jdbc.update("UPDATE zones SET name=? WHERE id=?", name, zoneId);
    }

    @Override
    public void updateZoneOrders(Long roomId, List<Long> zoneIdsInNewOrder) {
        for (int i = 0; i < zoneIdsInNewOrder.size(); i++) {
            jdbc.update(
                "UPDATE zones SET zone_order=? WHERE id=? AND room_id=?",
                i + 1, zoneIdsInNewOrder.get(i), roomId
            );
        }
    }
}
