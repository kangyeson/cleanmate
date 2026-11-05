package com.example.cleanmate.domain.auth.controller;

import com.example.cleanmate.domain.auth.dto.LoginRequest;
import com.example.cleanmate.domain.auth.service.AuthService;
import com.example.cleanmate.global.apiPayload.ApiResponse;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Duration;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private static final String SESSION_COOKIE = "CMSESSION";
    private static final int COOKIE_MAX_AGE = (int) Duration.ofDays(7).getSeconds();

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    /**
     * 로그인.
     * 성공 시 CMSESSION 쿠키(HttpOnly) 발급.
     */
    @PostMapping("/login")
    public ApiResponse<Void> login(
            @RequestBody LoginRequest request,
            HttpServletResponse response) {
        String sessionId = authService.login(request.name(), request.password());
        addSessionCookie(response, sessionId, COOKIE_MAX_AGE);
        return ApiResponse.ok();
    }

    /**
     * 로그아웃.
     * Redis 세션 키 삭제 + 쿠키 만료.
     */
    @PostMapping("/logout")
    public ApiResponse<Void> logout(
            @CookieValue(name = SESSION_COOKIE, required = false) String sessionId,
            HttpServletResponse response) {
        if (sessionId != null) {
            authService.logout(sessionId);
        }
        addSessionCookie(response, "", 0); // 쿠키 만료
        return ApiResponse.ok();
    }

    private void addSessionCookie(HttpServletResponse response, String value, int maxAge) {
        Cookie cookie = new Cookie(SESSION_COOKIE, value);
        cookie.setHttpOnly(true);
        cookie.setPath("/");
        cookie.setMaxAge(maxAge);
        response.addCookie(cookie);
    }
}
