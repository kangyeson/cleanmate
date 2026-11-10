package com.example.cleanmate.domain.auth.service;

import com.example.cleanmate.domain.auth.exception.InvalidCredentialsException;
import com.example.cleanmate.domain.member.model.Member;
import com.example.cleanmate.domain.member.repository.MemberRepository;
import com.example.cleanmate.global.security.PasswordHasher;
import com.example.cleanmate.global.session.SessionData;
import com.example.cleanmate.global.session.SessionStore;
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
                member.getRole().name(), member.getName()
        );
        return sessionStore.create(data);
    }

    /** 로그아웃 — Redis 키 삭제 */
    public void logout(String sessionId) {
        sessionStore.delete(sessionId);
    }
}
