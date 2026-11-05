package com.example.cleanmate.global.session;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.Optional;
import java.util.UUID;

/**
 * Redis 기반 세션 저장소.
 * key: session:{UUID}
 * value: JSON {"memberId":1,"roomId":1,"role":"MEMBER","name":"지현"}
 * TTL: 7일
 */
@Component
public class RedisSessionStore implements SessionStore {

    private static final String KEY_PREFIX = "session:";
    private static final Duration TTL = Duration.ofDays(7);

    private final StringRedisTemplate redis;
    private final ObjectMapper objectMapper;

    public RedisSessionStore(StringRedisTemplate redis, ObjectMapper objectMapper) {
        this.redis = redis;
        this.objectMapper = objectMapper;
    }

    @Override
    public String create(SessionData data) {
        String sessionId = UUID.randomUUID().toString();
        try {
            String json = objectMapper.writeValueAsString(data);
            redis.opsForValue().set(KEY_PREFIX + sessionId, json, TTL);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("세션 직렬화 실패", e);
        }
        return sessionId;
    }

    @Override
    public Optional<SessionData> find(String sessionId) {
        String json = redis.opsForValue().get(KEY_PREFIX + sessionId);
        if (json == null) return Optional.empty();
        try {
            return Optional.of(objectMapper.readValue(json, SessionData.class));
        } catch (JsonProcessingException e) {
            return Optional.empty();
        }
    }

    @Override
    public void delete(String sessionId) {
        redis.delete(KEY_PREFIX + sessionId);
    }
}
