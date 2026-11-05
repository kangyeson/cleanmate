package com.example.cleanmate.global.health;

import com.example.cleanmate.global.apiPayload.ApiResponse;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 헬스체크 엔드포인트.
 * Redis 연결 확인. DB 확인은 3단계(JDBC 저장소)에서 추가.
 */
@RestController
public class HealthController {

    private final StringRedisTemplate redis;

    public HealthController(StringRedisTemplate redis) {
        this.redis = redis;
    }

    @GetMapping("/health")
    public ResponseEntity<ApiResponse<Void>> health() {
        try {
            redis.opsForValue().get("__health_check__");
            return ResponseEntity.ok(ApiResponse.ok());
        } catch (Exception e) {
            return ResponseEntity.status(503).body(ApiResponse.error("Redis 연결 실패"));
        }
    }
}
