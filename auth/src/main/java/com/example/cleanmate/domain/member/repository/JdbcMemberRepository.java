package com.example.cleanmate.domain.member.repository;

import com.example.cleanmate.domain.member.model.Member;
import com.example.cleanmate.domain.member.model.MemberRole;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public class JdbcMemberRepository implements MemberRepository {

    private final JdbcTemplate jdbc;

    public JdbcMemberRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    private final RowMapper<Member> rowMapper = (rs, rowNum) -> new Member(
            rs.getLong("id"),
            rs.getLong("room_id"),
            rs.getString("name"),
            MemberRole.valueOf(rs.getString("role")),
            rs.getString("password_hash"),
            rs.getBoolean("must_change_pw"),
            rs.getInt("turn_order"),
            rs.getBoolean("active")
    );

    /**
     * 진행 중인 방(status='ACTIVE') 안에서만 조회.
     * 같은 이름의 멤버가 다른 방에 있을 수 있으므로 rooms JOIN 필요.
     */
    @Override
    public Optional<Member> findByName(String name) {
        List<Member> result = jdbc.query(
                """
                SELECT m.* FROM members m
                JOIN rooms r ON r.id = m.room_id
                WHERE m.name = ? AND r.status = 'ACTIVE' AND m.active = TRUE
                """,
                rowMapper, name
        );
        return result.stream().findFirst();
    }

    @Override
    public List<Member> findAllByRoomIdOrderByTurnOrder(Long roomId) {
        return jdbc.query(
                """
                SELECT * FROM members
                WHERE room_id = ? AND active = TRUE
                ORDER BY turn_order
                """,
                rowMapper, roomId
        );
    }
}
