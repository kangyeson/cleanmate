/*
 * redis.js — Redis 연결
 *
 * 연결을 두 개 둔다.
 *   client   : 세션 조회(GET session:{ID}), XACK, PING 같은 일반 명령
 *   blocking : 스트림 대기(XREADGROUP ... BLOCK). 대기하는 동안 연결을 붙잡고 있으므로
 *              일반 명령과 같은 연결을 쓰면 세션 조회가 대기에 막힌다.
 */
const { createClient } = require('redis');
const config = require('./config');

function makeClient(name) {
    const c = createClient({
        url: config.redisUrl,
        // 연결이 끊기면 1초 → 최대 5초 간격으로 다시 연결 시도
        socket: { reconnectStrategy: retries => Math.min(1000 * (retries + 1), 5000) },
    });
    // error 이벤트를 받지 않으면 연결 오류 때 프로세스가 종료된다. 로그만 남기고 재연결에 맡긴다.
    c.on('error', err => console.error(`[redis:${name}] ${err.message}`));
    return c;
}

const client = makeClient('main');
const blocking = makeClient('stream');

/** 서버 시작 시 두 연결을 연다. 실패하면 예외 → app.js가 종료하고 --restart on-failure로 다시 시작 */
async function connect() {
    await client.connect();
    await blocking.connect();
}

module.exports = { client, blocking, connect };
