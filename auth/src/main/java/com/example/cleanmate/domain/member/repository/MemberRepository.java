package com.example.cleanmate.domain.member.repository;

import com.example.cleanmate.domain.member.model.Member;

import java.util.List;
import java.util.Optional;

public interface MemberRepository {
    Optional<Member> findByName(String name);
    List<Member> findAllByRoomIdOrderByTurnOrder(Long roomId);
}
