/*
 * streamConsumer.js — 완료 이벤트(Redis Streams) 소비자
 *
 * 당번 서버(duty-1, duty-2 어느 쪽이든)가 완료 기록을 커밋한 뒤
 *   XADD duty-events * type DONE roomId 1 zoneName 화장실 doneByName 지현 doneCount 1 total 4 ...
 * 로 넣은 이벤트를 소비자 그룹 notify로 받아 알림함(notifications)에 저장한다.
 *
 * 왜 Pub/Sub이 아니라 Streams인가
 *   Pub/Sub은 그 순간 구독 중인 서버에만 전달된다. 알림 서버가 재시작·배포 중이면 그동안의
 *   소식이 사라진다. Streams는 이벤트가 스트림에 남아 있고, 소비자 그룹이 "어디까지 읽었는지"와
 *   "받았지만 처리(ACK)하지 못한 것(대기 목록)"을 기억하므로, 다시 켜지면 이어서 처리한다.
 *
 * 처리 순서 (반복)
 *   1) 대기 목록 먼저: XREADGROUP ... ID 0  → 전에 받았지만 ACK하지 못한 이벤트를 다시 처리
 *   2) 새 이벤트:     XREADGROUP ... ID >  BLOCK 5000 → 새 이벤트를 최대 5초 기다림
 *   알림함 저장에 성공한 이벤트만 XACK한다. DB 오류로 실패하면 ACK하지 않아 다음 반복에서 다시 처리.
 *
 * "적어도 한 번" 전달이라 같은 이벤트가 두 번 올 수 있다(저장 후 ACK 전에 멈춘 경우).
 * 알림함의 event_key UNIQUE(스트림 이벤트 ID)로 두 번째는 저장되지 않는다.
 */
const redis = require('./redis');
const db = require('./db');
const config = require('./config');
const { doneMessage } = require('./messages');

const { key: STREAM, group: GROUP, consumer: CONSUMER } = config.stream;

let running = false;
// 헬스 체크에서 보여 줄 상태
const status = { lastEventId: null, lastError: null, processed: 0 };

/** 소비자 그룹 만들기. 이미 있으면(BUSYGROUP) 그대로 쓴다. 스트림이 없으면 같이 만든다(MKSTREAM). */
async function ensureGroup() {
    try {
        // '0': 그룹을 처음 만들 때 스트림에 이미 있던 이벤트도 처리 대상으로 삼는다
        await redis.client.xGroupCreate(STREAM, GROUP, '0', { MKSTREAM: true });
        console.log(`[stream] 소비자 그룹 ${GROUP} 생성`);
    } catch (e) {
        if (!String(e.message).includes('BUSYGROUP')) throw e;
    }
}

/** 이벤트 하나 처리: 알림함에 저장 → 성공하면 ACK */
async function handle(message) {
    const e = message.message;   // 필드 (값은 모두 문자열)
    if (e.type === 'DONE') {
        const saved = await db.insertNotification({
            roomId: Number(e.roomId),
            memberId: null,                    // 방 전체 알림
            type: 'DONE',
            message: doneMessage(e),
            eventKey: `DONE:${message.id}`,    // 스트림 이벤트 ID로 중복 방지
        });
        console.log(`[stream] ${message.id} ${saved ? '저장' : '이미 저장됨(중복)'}: ${doneMessage(e)}`);
    }
    // 처리 완료 표시 (모르는 type도 ACK해서 대기 목록에 쌓이지 않게)
    await redis.blocking.xAck(STREAM, GROUP, message.id);
    status.lastEventId = message.id;
    status.processed++;
}

/** XREADGROUP 결과의 이벤트들을 차례로 처리. 하나라도 실패하면 예외 → 다음 반복에서 다시 */
async function handleAll(result) {
    if (!result) return 0;
    let count = 0;
    for (const s of result) {
        for (const message of s.messages) {
            if (!message) continue;   // 대기 목록에 있었지만 스트림 길이 제한으로 지워진 이벤트
            await handle(message);
            count++;
        }
    }
    return count;
}

async function loop() {
    while (running) {
        try {
            // 1) 대기 목록(받았지만 ACK 못 한 이벤트) 먼저
            const pending = await redis.blocking.xReadGroup(
                GROUP, CONSUMER, { key: STREAM, id: '0' }, { COUNT: 50 });
            const n = await handleAll(pending);
            if (n > 0) continue;   // 대기 목록이 남았을 수 있으니 다시 확인

            // 2) 새 이벤트를 최대 5초 기다림
            const fresh = await redis.blocking.xReadGroup(
                GROUP, CONSUMER, { key: STREAM, id: '>' }, { COUNT: 20, BLOCK: 5000 });
            await handleAll(fresh);
            status.lastError = null;
        } catch (e) {
            // DB·Redis 오류: ACK하지 않았으므로 이벤트는 대기 목록에 남는다. 잠시 뒤 다시 시도.
            status.lastError = e.message;
            console.error(`[stream] 처리 실패, 3초 뒤 다시 시도: ${e.message}`);
            await new Promise(r => setTimeout(r, 3000));
            try { await ensureGroup(); } catch (_) { /* Redis가 아직 안 됨 */ }
        }
    }
}

async function start() {
    await ensureGroup();
    running = true;
    console.log(`[stream] ${STREAM} 소비 시작 (그룹 ${GROUP}, 소비자 ${CONSUMER})`);
    loop();   // 백그라운드로 계속 실행
}

module.exports = { start, status };
