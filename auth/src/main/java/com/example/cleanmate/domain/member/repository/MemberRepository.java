package com.example.cleanmate.domain.member.repository;

import com.example.cleanmate.domain.member.model.Member;

import java.util.List;
import java.util.Optional;

public interface MemberRepository {
    Optional<Member> findByName(String name);
    Optional<Member> findById(Long id);
    List<Member> findAllByRoomIdOrderByTurnOrder(Long roomId);

    /** 멤버 저장 → DB에서 생성된 id가 채워진 Member 반환. */
    Member save(Member member);

    /** password_hash 갱신 + must_change_pw = FALSE 로 설정 */
    void updatePassword(Long memberId, String newHash);
}
