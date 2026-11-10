package com.example.cleanmate.global.health;

import com.example.cleanmate.global.apiPayload.ApiResponse;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class HealthController {

    private final StringRedisTemplate redis;
    private final JdbcTemplate jdbc;

    public HealthController(StringRedisTemplate redis, JdbcTemplate jdbc) {
        this.redis = redis;
        this.jdbc = jdbc;
    }

    @GetMapping("/health")
    public ResponseEntity<ApiResponse<Void>> health() {
        try {
            redis.opsForValue().get("__health_check__");
        } catch (Exception e) {
            return ResponseEntity.status(503).body(ApiResponse.error("Redis 연결 실패"));
        }
        try {
            jdbc.queryForObject("SELECT 1", Integer.class);
        } catch (Exception e) {
            return ResponseEntity.status(503).body(ApiResponse.error("DB 연결 실패"));
        }
        return ResponseEntity.ok(ApiResponse.ok());
    }
}
