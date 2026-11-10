package com.example.cleanmate.domain.room.repository;

import com.example.cleanmate.domain.room.exception.RoomAlreadyActiveException;
import com.example.cleanmate.domain.room.model.Room;
import com.example.cleanmate.domain.room.model.RoomStatus;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;

import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.Statement;
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

    @Override
    public Room save(Room room) {
        KeyHolder keyHolder = new GeneratedKeyHolder();
        try {
            jdbc.update(con -> {
                PreparedStatement ps = con.prepareStatement(
                        "INSERT INTO rooms (name, start_week, status) VALUES (?, ?, ?)",
                        Statement.RETURN_GENERATED_KEYS
                );
                ps.setString(1, room.getName());
                ps.setDate(2, Date.valueOf(room.getStartWeek()));
                ps.setString(3, room.getStatus().name());
                return ps;
            }, keyHolder);
        } catch (DuplicateKeyException e) {
            throw new RoomAlreadyActiveException();
        }
        long newId = keyHolder.getKey().longValue();
        return new Room(newId, room.getName(), room.getStartWeek(), room.getStatus());
    }
}
