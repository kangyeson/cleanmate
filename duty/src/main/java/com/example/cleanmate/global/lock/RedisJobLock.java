package com.example.cleanmate.global.lock;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.List;

/**
 * Redis로 구현한 정기 작업 잠금.
 *
 *   잠금:  SET lock:weekly-job {INSTANCE_ID} NX EX 120
 *          NX  = 키가 없을 때만 저장 → 두 서버가 동시에 보내도 한 대만 성공
 *          EX  = 120초 뒤 자동 삭제 → 작업 중 서버가 죽어도 잠금이 영원히 남지 않음
 *   해제:  값이 내 INSTANCE_ID일 때만 삭제 (아래 Lua 스크립트로 확인과 삭제를 한 번에)
 *          작업이 120초를 넘겨 잠금이 만료되고 다른 서버가 새로 잡은 경우,
 *          그 서버의 잠금을 지우지 않기 위해서다.
 *
 * 스케줄러 전용 라이브러리(ShedLock 등) 대신 이미 세션 저장소로 쓰는 Redis로 직접 구현했다.
 */
@Component
public class RedisJobLock implements JobLock {

    static final String KEY = "lock:weekly-job";
    private static final Duration TTL = Duration.ofSeconds(120);

    /** "값이 내 것일 때만 삭제"를 Redis 안에서 한 번에 실행 (확인과 삭제 사이에 끼어들 틈이 없음) */
    private static final DefaultRedisScript<Long> UNLOCK_SCRIPT = new DefaultRedisScript<>(
            "if redis.call('get', KEYS[1]) == ARGV[1] then return redis.call('del', KEYS[1]) else return 0 end",
            Long.class);

    private final StringRedisTemplate redis;
    private final String instanceId;

    public RedisJobLock(StringRedisTemplate redis,
                        @Value("${INSTANCE_ID:local}") String instanceId) {
        this.redis = redis;
        this.instanceId = instanceId;
    }

    @Override
    public boolean tryLock() {
        Boolean ok = redis.opsForValue().setIfAbsent(KEY, instanceId, TTL);
        return Boolean.TRUE.equals(ok);
    }

    @Override
    public void unlock() {
        redis.execute(UNLOCK_SCRIPT, List.of(KEY), instanceId);
    }
}
