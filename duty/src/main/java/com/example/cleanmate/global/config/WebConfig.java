package com.example.cleanmate.global.config;

import com.example.cleanmate.global.interceptor.SessionInterceptor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebConfig implements WebMvcConfigurer {

    private final SessionInterceptor sessionInterceptor;

    public WebConfig(SessionInterceptor sessionInterceptor) {
        this.sessionInterceptor = sessionInterceptor;
    }

    /**
     * 세션 인터셉터 적용 범위:
     *   - 기록 API: /done, /substitute, /record
     *   - 내 구역 조회: /mine
     * /week는 로그인 없이 공개.
     */
    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(sessionInterceptor)
                .addPathPatterns(
                        "/api/duty/*/done",
                        "/api/duty/*/substitute",
                        "/api/duty/*/record",
                        "/api/duty/mine"
                );
    }
}
