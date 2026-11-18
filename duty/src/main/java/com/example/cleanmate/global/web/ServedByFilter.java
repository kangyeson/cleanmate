package com.example.cleanmate.global.web;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * 모든 응답에 "어느 당번 서버가 처리했는지"와 "버전"을 헤더로 붙인다.
 *   X-Served-By:   duty-1 / duty-2 / duty-3 (환경 변수 INSTANCE_ID)
 *   X-App-Version: v1 / v2                 (환경 변수 APP_VERSION, 카나리·무중단 배포 확인용)
 */
@Component
public class ServedByFilter extends OncePerRequestFilter {

    private final String instanceId;
    private final String appVersion;

    public ServedByFilter(@Value("${INSTANCE_ID:local}") String instanceId,
                          @Value("${APP_VERSION:v1}") String appVersion) {
        this.instanceId = instanceId;
        this.appVersion = appVersion;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain chain) throws ServletException, IOException {
        // 컨트롤러보다 먼저 붙여 두어야 오류 응답(401·409·503)에도 헤더가 남는다
        response.setHeader("X-Served-By", instanceId);
        response.setHeader("X-App-Version", appVersion);
        chain.doFilter(request, response);
    }
}
