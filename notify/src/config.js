/*
 * config.js — 알림 서버 설정값
 *
 * 접속 정보는 이미지에 넣지 않고 컨테이너를 실행할 때 환경 변수로 받는다
 * (docker run --env-file env/notify.env -e DB_HOST=... ).
 * 개발 PC에서 직접 실행할 때를 위해 기본값을 둔다.
 */

module.exports = {
    // 이 서버가 받는 포트 (프록시의 /api/notify → cleanmate-notify:3000)
    port: Number(process.env.PORT || 3000),

    // Redis: 세션 읽기(session:{ID})와 완료 이벤트 스트림(duty-events)
    redisUrl: `redis://${process.env.REDIS_HOST || 'localhost'}:${process.env.REDIS_PORT || 6379}`,

    // MySQL: 알림 서버 전용 스키마와 계정 (다른 스키마에는 권한 없음)
    db: {
        host: process.env.DB_HOST || 'localhost',
        port: Number(process.env.DB_PORT || 3306),
        user: process.env.DB_USER || 'notify_user',
        password: process.env.DB_PASSWORD,
        database: 'cleanmate_notify',
    },

    // 서버 간 호출 주소 (프록시를 거치지 않는 내부 주소)
    authUrl: process.env.AUTH_URL || 'http://localhost:8081',   // GET /internal/roster
    dutyUrl: process.env.DUTY_URL || 'http://localhost:8082',   // GET /api/duty/week

    // 완료 이벤트 스트림 (당번 서버의 RedisStreamEventPublisher와 같은 값)
    stream: {
        key: 'duty-events',
        group: 'notify',
        // 소비자 이름은 컨테이너가 다시 만들어져도 같아야 한다.
        // 그래야 재시작 전에 받고 처리(ACK)하지 못한 이벤트를 이어서 처리할 수 있다.
        consumer: process.env.CONSUMER_NAME || 'notify-1',
    },

    // 미완료 리마인더 시각: 수·토 20시 (시험할 때는 REMIND_CRON="* * * * *"로 1분마다)
    remindCron: process.env.REMIND_CRON || '0 20 * * 3,6',
    timezone: 'Asia/Seoul',
};
