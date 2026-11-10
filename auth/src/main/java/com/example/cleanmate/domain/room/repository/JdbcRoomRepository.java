package com.example.cleanmate.domain.room.repository;

import com.example.cleanmate.domain.room.model.Room;
import com.example.cleanmate.domain.room.model.RoomStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public class JdbcRoomRepository implements RoomRepository {

    private final JdbcTemplate jdbc;

    public JdbcRoomRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    private final RowMapper<Room> rowMapper = (rs, rowNum) -> new Room(
            rs.getLong("id"),
            rs.getString("name"),
            rs.getDate("start_week").toLocalDate(),
            RoomStatus.valueOf(rs.getString("status"))
    );

    @Override
    public Optional<Room> findActive() {
        List<Room> result = jdbc.query(
                "SELECT * FROM rooms WHERE status = 'ACTIVE'",
                rowMapper
        );
        return result.stream().findFirst();
    }
}
