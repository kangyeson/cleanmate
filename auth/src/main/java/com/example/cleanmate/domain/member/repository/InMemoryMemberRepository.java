package com.example.cleanmate.domain.member.repository;

import com.example.cleanmate.domain.member.model.Member;
import com.example.cleanmate.domain.member.model.MemberRole;
import com.example.cleanmate.global.security.PasswordHasher;

import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * 단위 테스트 전용 메모리 MemberRepository. Spring 빈으로 등록하지 않는다.
 * 프로덕션에서는 JdbcMemberRepository가 MemberRepository 역할을 맡는다.
 */
public class InMemoryMemberRepository implements MemberRepository {

    private final Map<Long, Member> store = new HashMap<>();
    private long nextId = 1L;

    /** 테스트용 빈 저장소 (RoomServiceTest에서 사용). */
    public InMemoryMemberRepository() {}

    /** AuthServiceTest용 초기 데이터 세팅 생성자. */
    public InMemoryMemberRepository(PasswordHasher hasher) {
        add(1L, "민정", MemberRole.MEMBER, 1, hasher);
        add(2L, "지현", MemberRole.MEMBER, 2, hasher);
        add(3L, "서현", MemberRole.MEMBER, 3, hasher);
        add(4L, "예손", MemberRole.ADMIN,  4, hasher);
        nextId = 5L;
    }

    private void add(Long id, String name, MemberRole role, int order, PasswordHasher hasher) {
        store.put(id, new Member(id, 1L, name, role, hasher.hash("1234"), false, order, true));
    }

    @Override
    public Optional<Member> findById(Long id) {
        return Optional.ofNullable(store.get(id));
    }

    @Override
    public Optional<Member> findByName(String name) {
        return store.values().stream()
                .filter(m -> m.getName().equals(name) && m.isActive())
                .findFirst();
    }

    @Override
    public List<Member> findAllByRoomIdOrderByTurnOrder(Long roomId) {
        return store.values().stream()
                .filter(m -> m.getRoomId().equals(roomId) && m.isActive())
                .sorted(Comparator.comparingInt(Member::getTurnOrder))
                .toList();
    }

    @Override
    public Member save(Member member) {
        long id = nextId++;
        Member saved = new Member(id, member.getRoomId(), member.getName(), member.getRole(),
                member.getPasswordHash(), member.isMustChangePw(), member.getTurnOrder(), member.isActive());
        store.put(id, saved);
        return saved;
    }

    @Override
    public void updatePassword(Long memberId, String newHash) {
        Member existing = store.get(memberId);
        if (existing == null) return;
        Member updated = new Member(
            existing.getId(), existing.getRoomId(), existing.getName(), existing.getRole(),
            newHash, false, existing.getTurnOrder(), existing.isActive()
        );
        store.put(memberId, updated);
    }

    /** 테스트용: 저장된 전체 멤버 목록 반환. */
    public List<Member> findAll() {
        return List.copyOf(store.values());
    }
}
