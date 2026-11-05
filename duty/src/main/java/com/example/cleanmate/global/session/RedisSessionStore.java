package com.example.cleanmate.global.session;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.util.Optional;

/**
 * auth 서버가 저장한 session:{UUID} 키를 읽는다.
 * 쓰기·삭제 없음.
 */
@Component
public class RedisSessionStore implements SessionStore {

    private static final String KEY_PREFIX = "session:";

    private final StringRedisTemplate redis;
    private final ObjectMapper objectMapper;

    public RedisSessionStore(StringRedisTemplate redis, ObjectMapper objectMapper) {
        this.redis = redis;
        this.objectMapper = objectMapper;
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
}
