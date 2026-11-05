package com.example.cleanmate.domain.member.repository;

import com.example.cleanmate.domain.member.model.Member;
import com.example.cleanmate.domain.member.model.MemberRole;
import com.example.cleanmate.global.security.PasswordHasher;
import org.springframework.stereotype.Repository;

import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * 개발·테스트용 메모리 MemberRepository.
 * 3단계에서 JdbcMemberRepository로 교체.
 *
 * 초기 비밀번호: 모두 "1234"
 * ID는 StubRosterClient(duty 서버)와 동일하게 맞춤.
 */
@Repository
public class InMemoryMemberRepository implements MemberRepository {

    private final Map<Long, Member> store = new HashMap<>();

    public InMemoryMemberRepository(PasswordHasher hasher) {
        // 고정 salt — 재시작해도 비밀번호 "1234"로 로그인 가능
        add(1L, "민정", MemberRole.ADMIN,  "salt-minjung",  1, hasher);
        add(2L, "지현", MemberRole.MEMBER, "salt-jihyeon",  2, hasher);
        add(3L, "서현", MemberRole.MEMBER, "salt-seohyeon", 3, hasher);
        add(4L, "예손", MemberRole.MEMBER, "salt-yeson",    4, hasher);
    }

    private void add(Long id, String name, MemberRole role, String salt, int order, PasswordHasher hasher) {
        store.put(id, new Member(id, 1L, name, role, salt, hasher.hash("1234", salt), false, order, true));
    }

    @Override
    public Optional<Member> findByName(String name) {
        return store.values().stream()
                .filter(m -> m.getName().equals(name))
                .findFirst();
    }

    @Override
    public List<Member> findAllByRoomIdOrderByTurnOrder(Long roomId) {
        return store.values().stream()
                .filter(m -> m.getRoomId().equals(roomId) && m.isActive())
                .sorted(Comparator.comparingInt(Member::getTurnOrder))
                .toList();
    }
}
