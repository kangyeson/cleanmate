package com.example.cleanmate.global.interceptor;

import com.example.cleanmate.global.exception.PasswordChangeRequiredException;
import com.example.cleanmate.global.exception.UnauthorizedException;
import com.example.cleanmate.global.session.SessionData;
import com.example.cleanmate.global.session.SessionStore;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

@Component
public class SessionInterceptor implements HandlerInterceptor {

    /** 컨트롤러에서 memberId를 꺼낼 때 쓰는 request attribute 키 */
    public static final String MEMBER_ATTR = "memberId";

    private static final String SESSION_COOKIE = "CMSESSION";

    private final SessionStore sessionStore;

    public SessionInterceptor(SessionStore sessionStore) {
        this.sessionStore = sessionStore;
    }

    @Override
    public boolean preHandle(HttpServletRequest request,
                             HttpServletResponse response,
                             Object handler) {
        String sessionId = extractCookie(request);
        if (sessionId == null) throw new UnauthorizedException();

        SessionData session = sessionStore.find(sessionId)
                .orElseThrow(UnauthorizedException::new);

        if (Boolean.TRUE.equals(session.mustChangePw()) && isMutationRequest(request)) {
            throw new PasswordChangeRequiredException();
        }

        request.setAttribute(MEMBER_ATTR, session.memberId());
        return true;
    }

    private boolean isMutationRequest(HttpServletRequest request) {
        String method = request.getMethod();
        return "POST".equals(method) || "DELETE".equals(method);
    }

    private String extractCookie(HttpServletRequest request) {
        Cookie[] cookies = request.getCookies();
        if (cookies == null) return null;
        for (Cookie c : cookies) {
            if (SESSION_COOKIE.equals(c.getName())) return c.getValue();
        }
        return null;
    }
}
