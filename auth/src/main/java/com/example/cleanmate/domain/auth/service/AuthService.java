package com.example.cleanmate.domain.auth.service;

import com.example.cleanmate.domain.auth.dto.MeResponse;
import com.example.cleanmate.domain.auth.exception.InvalidCredentialsException;
import com.example.cleanmate.domain.member.model.Member;
import com.example.cleanmate.domain.member.repository.MemberRepository;
import com.example.cleanmate.global.exception.CleanmateException;
import com.example.cleanmate.global.security.PasswordHasher;
import com.example.cleanmate.global.session.SessionData;
import com.example.cleanmate.global.session.SessionStore;
import com.example.cleanmate.global.validation.InputValidator;
import org.springframework.stereotype.Service;

@Service
public class AuthService {

    private final MemberRepository memberRepository;
    private final PasswordHasher passwordHasher;
    private final SessionStore sessionStore;

    public AuthService(MemberRepository memberRepository,
                       PasswordHasher passwordHasher,
                       SessionStore sessionStore) {
        this.memberRepository = memberRepository;
        this.passwordHasher = passwordHasher;
        this.sessionStore = sessionStore;
    }

    /**
     * 로그인 성공 시 세션 UUID 반환.
     * 실패 시 InvalidCredentialsException (401).
     */
    public String login(String name, String password) {
        Member member = memberRepository.findByName(name)
                .orElseThrow(InvalidCredentialsException::new);

        if (!member.isActive()) {
            throw new InvalidCredentialsException();
        }
        if (!passwordHasher.matches(password, member.getPasswordHash())) {
            throw new InvalidCredentialsException();
        }

        SessionData data = new SessionData(
                member.getId(), member.getRoomId(),
                member.getRole().name(), member.getName(),
                member.isMustChangePw()
        );
        return sessionStore.create(data);
    }

    /** 로그아웃 — Redis 키 삭제 */
    public void logout(String sessionId) {
        sessionStore.delete(sessionId);
    }

    /**
     * 현재 로그인한 사용자 정보 반환.
     * sessionId 가 null 이거나 Redis 에 세션이 없으면 401.
     */
    public MeResponse currentUser(String sessionId) {
        if (sessionId == null) {
            throw new CleanmateException("로그인이 필요합니다.", 401);
        }
        SessionData data = sessionStore.find(sessionId)
                .orElseThrow(() -> new CleanmateException("로그인이 필요합니다.", 401));
        return new MeResponse(data.memberId(), data.name(), data.role(),
            Boolean.TRUE.equals(data.mustChangePw()));
    }

    /**
     * 비밀번호 변경 + must_change_pw = false.
     * 성공 시 Redis 세션을 갱신.
     */
    public void changePassword(String sessionId, String currentPassword, String newPassword) {
        if (sessionId == null) {
            throw new CleanmateException("로그인이 필요합니다.", 401);
        }
        SessionData session = sessionStore.find(sessionId)
            .orElseThrow(() -> new CleanmateException("로그인이 필요합니다.", 401));

        Member member = memberRepository.findById(session.memberId())
            .orElseThrow(() -> new CleanmateException("회원을 찾을 수 없습니다.", 404));

        if (!passwordHasher.matches(currentPassword, member.getPasswordHash())) {
            throw new CleanmateException("현재 비밀번호가 올바르지 않습니다.", 400);
        }
        InputValidator.validatePassword(newPassword);

        memberRepository.updatePassword(member.getId(), passwordHasher.hash(newPassword));

        // Redis 세션의 mustChangePw 갱신
        SessionData updated = new SessionData(
            session.memberId(), session.roomId(), session.role(), session.name(), false
        );
        sessionStore.update(sessionId, updated);
    }
}
