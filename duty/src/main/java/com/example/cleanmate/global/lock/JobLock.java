package com.example.cleanmate.global.lock;

/**
 * 여러 서버 중 한 대만 작업하도록 하는 잠금.
 *
 * 당번 서버를 2대(duty-1, duty-2)로 늘리면 매주 월요일 0시 정기 작업이 두 대에서 동시에
 * 시작된다. 작업 전에 tryLock()으로 잠금을 얻은 서버만 실행하고, 못 얻은 서버는 건너뛴다.
 *
 * 운영: RedisJobLock (Redis SET NX EX), 단위 테스트: 가짜 구현으로 바꿔 끼움.
 */
public interface JobLock {

    /** 잠금을 얻으면 true, 다른 서버가 이미 가지고 있으면 false */
    boolean tryLock();

    /** 내가 얻은 잠금만 푼다 (다른 서버의 잠금은 건드리지 않음) */
    void unlock();
}
