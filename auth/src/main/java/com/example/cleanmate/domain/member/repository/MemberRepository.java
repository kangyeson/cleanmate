package com.example.cleanmate.domain.member.repository;

import com.example.cleanmate.domain.member.model.Member;

import java.util.List;
import java.util.Optional;

public interface MemberRepository {
    Optional<Member> findByName(String name);
    Optional<Member> findById(Long id);

    /** 해당 방의 모든 멤버(비활성 포함), turn_order 오름차순. */
    List<Member> findAllByRoomId(Long roomId);

    /** 멤버 저장 → DB에서 생성된 id가 채워진 Member 반환. */
    Member save(Member member);

    /** password_hash 갱신 + must_change_pw = FALSE 로 설정 */
    void updatePassword(Long memberId, String newHash);

    /** password_hash 갱신 + must_change_pw = TRUE 로 설정 (관리자 초기화용) */
    void resetPassword(Long memberId, String newHash);

    /** 멤버 비활성화 (active = FALSE) */
    void deactivate(Long memberId);

    /** 이름 변경 */
    void updateName(Long memberId, String name);

    /** turn_order를 리스트 순서(1부터)로 일괄 갱신 */
    void updateTurnOrders(Long roomId, List<Long> memberIdsInNewOrder);
}
