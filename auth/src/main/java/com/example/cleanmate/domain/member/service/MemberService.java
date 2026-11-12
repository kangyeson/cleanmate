package com.example.cleanmate.domain.member.service;

import com.example.cleanmate.domain.member.dto.MemberResponse;
import com.example.cleanmate.domain.member.model.Member;
import com.example.cleanmate.domain.member.model.MemberRole;
import com.example.cleanmate.domain.member.repository.MemberRepository;
import com.example.cleanmate.domain.client.DutyClient;
import com.example.cleanmate.global.exception.CleanmateException;
import com.example.cleanmate.global.security.PasswordHasher;
import com.example.cleanmate.global.session.AdminGuard;
import com.example.cleanmate.global.session.SessionData;
import com.example.cleanmate.global.validation.InputValidator;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
public class MemberService {

    private final MemberRepository memberRepository;
    private final PasswordHasher passwordHasher;
    private final AdminGuard adminGuard;
    private final DutyClient dutyClient;

    public MemberService(MemberRepository memberRepository,
                         PasswordHasher passwordHasher,
                         AdminGuard adminGuard,
                         DutyClient dutyClient) {
        this.memberRepository = memberRepository;
        this.passwordHasher = passwordHasher;
        this.adminGuard = adminGuard;
        this.dutyClient = dutyClient;
    }

    public List<MemberResponse> getMembers(String sessionId) {
        SessionData session = adminGuard.requireAdmin(sessionId);
        return memberRepository.findAllByRoomId(session.roomId()).stream()
            .map(m -> new MemberResponse(
                m.getId(), m.getName(), m.getRole().name(),
                m.getTurnOrder(), m.isActive(), m.isMustChangePw()))
            .toList();
    }

    @Transactional
    public void addMember(String sessionId, String name, String password) {
        SessionData session = adminGuard.requireAdmin(sessionId);
        Long roomId = session.roomId();
        dutyClient.ensureCurrentWeek(roomId);  // 배정 먼저 확정
        String validatedName = InputValidator.validateName(name);
        InputValidator.validatePassword(password);

        List<Member> all = memberRepository.findAllByRoomId(roomId);
        int maxOrder = all.stream().mapToInt(Member::getTurnOrder).max().orElse(0);

        String hash = passwordHasher.hash(password);
        try {
            memberRepository.save(new Member(
                null, roomId, validatedName, MemberRole.MEMBER,
                hash, true, maxOrder + 1, true
            ));
        } catch (DuplicateKeyException e) {
            throw new CleanmateException("이미 사용된 이름입니다.", 400);
        }
    }

    @Transactional
    public void renameMember(String sessionId, Long memberId, String newName) {
        SessionData session = adminGuard.requireAdmin(sessionId);
        Long roomId = session.roomId();
        String validatedName = InputValidator.validateName(newName);

        Member member = memberRepository.findById(memberId)
            .orElseThrow(() -> new CleanmateException("멤버를 찾을 수 없습니다.", 404));
        if (!member.getRoomId().equals(roomId)) {
            throw new CleanmateException("멤버를 찾을 수 없습니다.", 404);
        }

        try {
            memberRepository.updateName(memberId, validatedName);
        } catch (DuplicateKeyException e) {
            throw new CleanmateException("이미 사용된 이름입니다.", 400);
        }
    }

    @Transactional
    public void deleteMember(String sessionId, Long memberId) {
        SessionData session = adminGuard.requireAdmin(sessionId);
        Long roomId = session.roomId();
        dutyClient.ensureCurrentWeek(roomId);  // 배정 먼저 확정

        Member member = memberRepository.findById(memberId)
            .orElseThrow(() -> new CleanmateException("멤버를 찾을 수 없습니다.", 404));
        if (!member.getRoomId().equals(roomId)) {
            throw new CleanmateException("멤버를 찾을 수 없습니다.", 404);
        }
        if (!member.isActive()) {
            throw new CleanmateException("이미 비활성화된 멤버입니다.", 400);
        }
        if (member.getId().equals(session.memberId())) {
            throw new CleanmateException("본인 계정은 삭제할 수 없습니다.", 400);
        }
        long activeCount = memberRepository.findAllByRoomId(roomId).stream()
            .filter(Member::isActive).count();
        if (activeCount <= 1) {
            throw new CleanmateException("마지막 활성 멤버는 삭제할 수 없습니다.", 400);
        }

        memberRepository.deactivate(memberId);
    }

    @Transactional
    public void reorderMembers(String sessionId, List<Long> memberIds) {
        SessionData session = adminGuard.requireAdmin(sessionId);
        Long roomId = session.roomId();
        dutyClient.ensureCurrentWeek(roomId);  // 배정 먼저 확정

        List<Long> activeIds = memberRepository.findAllByRoomId(roomId).stream()
            .filter(Member::isActive)
            .map(Member::getId)
            .toList();

        if (memberIds.size() != activeIds.size()) {
            throw new CleanmateException("활성 멤버 목록과 ID 수가 일치하지 않습니다.", 400);
        }
        Set<Long> activeSet = new HashSet<>(activeIds);
        for (Long id : memberIds) {
            if (!activeSet.contains(id)) {
                throw new CleanmateException("유효하지 않은 멤버 ID가 포함되어 있습니다.", 400);
            }
        }
        if (new HashSet<>(memberIds).size() != memberIds.size()) {
            throw new CleanmateException("중복된 멤버 ID가 있습니다.", 400);
        }

        memberRepository.updateTurnOrders(roomId, memberIds);
    }

    @Transactional
    public void resetPassword(String sessionId, Long memberId, String newPassword) {
        SessionData session = adminGuard.requireAdmin(sessionId);
        Long roomId = session.roomId();

        Member member = memberRepository.findById(memberId)
            .orElseThrow(() -> new CleanmateException("멤버를 찾을 수 없습니다.", 404));
        if (!member.getRoomId().equals(roomId)) {
            throw new CleanmateException("멤버를 찾을 수 없습니다.", 404);
        }

        InputValidator.validatePassword(newPassword);
        String hash = passwordHasher.hash(newPassword);
        memberRepository.resetPassword(memberId, hash);
    }
}
