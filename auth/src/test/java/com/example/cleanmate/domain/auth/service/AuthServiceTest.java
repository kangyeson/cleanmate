package com.example.cleanmate.domain.auth.service;

import com.example.cleanmate.domain.auth.exception.InvalidCredentialsException;
import com.example.cleanmate.domain.member.repository.InMemoryMemberRepository;
import com.example.cleanmate.global.security.PasswordHasher;
import com.example.cleanmate.global.session.SessionData;
import com.example.cleanmate.global.session.SessionStore;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class AuthServiceTest {

    // 초기 비밀번호 "1234", id: 민정=1, 지현=2, 서현=3, 예손=4
    private AuthService service;
    private InMemorySessionStore sessionStore;

    @BeforeEach
    void setUp() {
        PasswordHasher hasher = new PasswordHasher();
        sessionStore = new InMemorySessionStore();
        service = new AuthService(
                new InMemoryMemberRepository(hasher),
                hasher,
                sessionStore
        );
    }

    // ── 성공 ──────────────────────────────────────────────────

    @Test
    void login_성공_sessionId_반환() {
        String sessionId = service.login("지현", "1234");
        assertNotNull(sessionId);
        assertFalse(sessionId.isBlank());
    }

    @Test
    void login_성공_세션에_올바른_정보_저장() {
        String sessionId = service.login("지현", "1234");

        SessionData data = sessionStore.find(sessionId).orElseThrow();
        assertEquals(2L,      data.memberId());
        assertEquals(1L,      data.roomId());
        assertEquals("지현",  data.name());
        assertEquals("MEMBER", data.role());
    }

    @Test
    void login_성공_관리자_role_ADMIN() {
        String sessionId = service.login("예손", "1234");

        SessionData data = sessionStore.find(sessionId).orElseThrow();
        assertEquals(4L,      data.memberId());
        assertEquals("ADMIN", data.role());
    }

    // ── 실패 ──────────────────────────────────────────────────

    @Test
    void login_틀린_비밀번호_401() {
        assertThrows(InvalidCredentialsException.class,
                () -> service.login("지현", "wrong"));
    }

    @Test
    void login_없는_이름_401() {
        assertThrows(InvalidCredentialsException.class,
                () -> service.login("없는사람", "1234"));
    }

    @Test
    void login_빈_비밀번호_401() {
        assertThrows(InvalidCredentialsException.class,
                () -> service.login("지현", ""));
    }

    // ── 로그아웃 ──────────────────────────────────────────────

    @Test
    void logout_세션_삭제() {
        String sessionId = service.login("지현", "1234");
        service.logout(sessionId);
        assertTrue(sessionStore.find(sessionId).isEmpty());
    }

    // ── 테스트용 인메모리 SessionStore ───────────────────────

    static class InMemorySessionStore implements SessionStore {
        private final Map<String, SessionData> store = new HashMap<>();

        @Override
        public String create(SessionData data) {
            String id = UUID.randomUUID().toString();
            store.put(id, data);
            return id;
        }

        @Override
        public Optional<SessionData> find(String sessionId) {
            return Optional.ofNullable(store.get(sessionId));
        }

        @Override
        public void delete(String sessionId) {
            store.remove(sessionId);
        }
    }
}
