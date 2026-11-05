package com.example.cleanmate.global.interceptor;

import com.example.cleanmate.global.exception.UnauthorizedException;
import com.example.cleanmate.global.session.SessionData;
import com.example.cleanmate.global.session.SessionStore;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

class SessionInterceptorTest {

    // 고정 세션 UUID와 대응하는 SessionData
    private static final String VALID_ID  = "valid-session-uuid";
    private static final SessionData SESSION = new SessionData(2L, 1L, "MEMBER", "지현");

    private final SessionStore stubStore = sessionId ->
            VALID_ID.equals(sessionId) ? Optional.of(SESSION) : Optional.empty();

    private final SessionInterceptor interceptor = new SessionInterceptor(stubStore);

    // ── 401 케이스 ────────────────────────────────────────────

    @Test
    void 쿠키_자체가_없으면_401() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        // 쿠키 미설정
        assertThrows(UnauthorizedException.class,
                () -> interceptor.preHandle(request, new MockHttpServletResponse(), null));
    }

    @Test
    void CMSESSION_쿠키가_없고_다른_쿠키만_있으면_401() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setCookies(new Cookie("OTHER_COOKIE", "some-value"));
        assertThrows(UnauthorizedException.class,
                () -> interceptor.preHandle(request, new MockHttpServletResponse(), null));
    }

    @Test
    void 유효하지_않은_sessionId면_401() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setCookies(new Cookie("CMSESSION", "invalid-uuid"));
        assertThrows(UnauthorizedException.class,
                () -> interceptor.preHandle(request, new MockHttpServletResponse(), null));
    }

    // ── 성공 케이스 ───────────────────────────────────────────

    @Test
    void 유효한_세션이면_preHandle_true_반환() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setCookies(new Cookie("CMSESSION", VALID_ID));
        boolean result = interceptor.preHandle(request, new MockHttpServletResponse(), null);
        assertTrue(result);
    }

    @Test
    void 유효한_세션이면_request에_memberId_저장() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setCookies(new Cookie("CMSESSION", VALID_ID));
        interceptor.preHandle(request, new MockHttpServletResponse(), null);
        assertEquals(2L, request.getAttribute(SessionInterceptor.MEMBER_ATTR));
    }
}
