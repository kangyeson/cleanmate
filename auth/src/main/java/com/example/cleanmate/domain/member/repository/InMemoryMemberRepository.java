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
 *
 * TODO(3단계): JdbcMemberRepository에서는 고정 salt 대신
 *   hasher.generateSalt()로 사람마다 다른 salt를 생성하고 DB에 저장할 것.
 *   현재 고정 salt는 메모리 저장소 전용 편의 설정이므로 절대 JDBC 구현에 복사하지 말 것.
 */
@Repository
public class InMemoryMemberRepository implements MemberRepository {

    private final Map<Long, Member> store = new HashMap<>();

    public InMemoryMemberRepository(PasswordHasher hasher) {
        // 고정 salt — 재시작해도 비밀번호 "1234"로 로그인 가능 (메모리 한정)
        add(1L, "민정", MemberRole.MEMBER, "salt-minjung",  1, hasher);
        add(2L, "지현", MemberRole.MEMBER, "salt-jihyeon",  2, hasher);
        add(3L, "서현", MemberRole.MEMBER, "salt-seohyeon", 3, hasher);
        add(4L, "예손", MemberRole.ADMIN,  "salt-yeson",    4, hasher);
    }

    private void add(Long id, String name, MemberRole role, String salt, int order, PasswordHasher hasher) {
        store.put(id, new Member(id, 1L, name, role, salt, hasher.hash("1234", salt), false, order, true));
    }

    @Override
    public Optional<Member> findByName(String name) {
        // TODO(3단계): JdbcMemberRepository에서는 현재 활성 방(roomId)도 함께 필터링할 것.
        //   같은 이름의 멤버가 다른 방에 있을 수 있으므로 name + roomId 조합으로 조회해야 함.
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
