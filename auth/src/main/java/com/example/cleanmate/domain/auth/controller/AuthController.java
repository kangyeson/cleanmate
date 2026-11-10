package com.example.cleanmate.domain.auth.controller;

import com.example.cleanmate.domain.auth.dto.LoginRequest;
import com.example.cleanmate.domain.auth.service.AuthService;
import com.example.cleanmate.global.apiPayload.ApiResponse;
import com.example.cleanmate.global.web.SessionCookie;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

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
            @Valid @RequestBody LoginRequest request,
            HttpServletResponse response) {
        String sessionId = authService.login(request.name(), request.password());
        SessionCookie.addTo(response, sessionId, SessionCookie.MAX_AGE);
        return ApiResponse.ok();
    }

    /**
     * 로그아웃.
     * Redis 세션 키 삭제 + 쿠키 만료.
     */
    @PostMapping("/logout")
    public ApiResponse<Void> logout(
            @CookieValue(name = SessionCookie.NAME, required = false) String sessionId,
            HttpServletResponse response) {
        if (sessionId != null) {
            authService.logout(sessionId);
        }
        SessionCookie.addTo(response, "", 0); // 쿠키 만료
        return ApiResponse.ok();
    }
}
