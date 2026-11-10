package com.example.cleanmate.global.web;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletResponse;

import java.time.Duration;

/**
 * CMSESSION 쿠키 생성 유틸리티.
 * AuthController와 RoomController에서 공통으로 사용한다.
 */
public final class SessionCookie {

    public static final String NAME = "CMSESSION";
    public static final int MAX_AGE = (int) Duration.ofDays(7).getSeconds();

    private SessionCookie() {}

    public static void addTo(HttpServletResponse response, String value, int maxAge) {
        Cookie cookie = new Cookie(NAME, value);
        cookie.setHttpOnly(true);
        cookie.setPath("/");
        cookie.setMaxAge(maxAge);
        response.addCookie(cookie);
    }
}
