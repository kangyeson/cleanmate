package com.example.cleanmate.domain.member.repository;

import com.example.cleanmate.domain.member.model.Member;
import com.example.cleanmate.domain.member.model.MemberRole;
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
    public Optional<Member> findById(Long id) {
        List<Member> result = jdbc.query(
                "SELECT * FROM members WHERE id = ?",
                rowMapper, id
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

    @Override
    public Member save(Member member) {
        KeyHolder keyHolder = new GeneratedKeyHolder();
        jdbc.update(con -> {
            PreparedStatement ps = con.prepareStatement(
                    """
                    INSERT INTO members (room_id, name, role, password_hash, must_change_pw, turn_order, active)
                    VALUES (?, ?, ?, ?, ?, ?, ?)
                    """,
                    Statement.RETURN_GENERATED_KEYS
            );
            ps.setLong(1, member.getRoomId());
            ps.setString(2, member.getName());
            ps.setString(3, member.getRole().name());
            ps.setString(4, member.getPasswordHash());
            ps.setBoolean(5, member.isMustChangePw());
            ps.setInt(6, member.getTurnOrder());
            ps.setBoolean(7, member.isActive());
            return ps;
        }, keyHolder);
        long newId = keyHolder.getKey().longValue();
        return new Member(newId, member.getRoomId(), member.getName(), member.getRole(),
                member.getPasswordHash(), member.isMustChangePw(), member.getTurnOrder(), member.isActive());
    }

    @Override
    public void updatePassword(Long memberId, String newHash) {
        jdbc.update(
            "UPDATE members SET password_hash=?, must_change_pw=FALSE WHERE id=?",
            newHash, memberId
        );
    }
}
