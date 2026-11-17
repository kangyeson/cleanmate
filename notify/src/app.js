/*
 * app.js — 청소 당번 도우미 알림 서버 (Node.js Express)
 *
 * 맡는 일
 *   - 완료 이벤트 소비: Redis Streams(duty-events) → 알림함 저장   (streamConsumer.js)
 *   - 미완료 리마인더: 수·토 20시, 미완료 담당자에게 알림함 항목     (reminder.js)
 *   - 알림함·설정 API: 화면(notify.html, 상단 "알림" 배지)이 사용
 *
 * API (프록시가 /api/notify/* 를 cleanmate-notify:3000 으로 전달, 로그인 필요)
 *   GET  /api/notify/inbox           → { unread, items: [{ id, type, message, createdAt, unread }] }
 *   GET  /api/notify/inbox/unread    → { unread }           (상단 배지용, 가벼운 조회)
 *   POST /api/notify/inbox/read      → 모두 읽음 표시
 *   GET  /api/notify/settings        → { remindOn }
 *   PUT  /api/notify/settings        ← { remindOn: true|false }
 *   GET  /health                     → Redis·DB 연결 확인 (200 / 503), 프록시에 등록하지 않음
 *
 * 로그인 상태는 Java 인증 서버가 만든 Redis 세션(JSON)을 그대로 읽는다(session.js).
 */
const express = require('express');
const config = require('./config');
const redis = require('./redis');
const db = require('./db');
const { requireLogin } = require('./session');
const { ok, fail } = require('./response');
const streamConsumer = require('./streamConsumer');
const reminder = require('./reminder');

const app = express();
app.use(express.json({ limit: '10kb' }));

// 응답 헤더에 처리한 서버를 표시 (당번 서버의 X-Served-By와 같은 방식)
app.use((req, res, next) => {
    res.set('X-Served-By', 'notify');
    next();
});

/** async 핸들러에서 난 오류를 한곳(아래 오류 처리)으로 보낸다 */
const wrap = fn => (req, res, next) => fn(req, res, next).catch(next);

// ── 알림함 ──────────────────────────────────────────────
app.get('/api/notify/inbox', requireLogin, wrap(async (req, res) => {
    const { roomId, memberId } = req.session;
    const [rows, unread] = await Promise.all([
        db.getInbox(roomId, memberId),
        db.countUnread(roomId, memberId),
    ]);
    // 안 읽은 것 표시: 최신 unread개가 안 읽은 알림
    const items = rows.map((r, i) => ({ ...r, unread: i < unread }));
    ok(res, { unread, items });
}));

app.get('/api/notify/inbox/unread', requireLogin, wrap(async (req, res) => {
    const { roomId, memberId } = req.session;
    ok(res, { unread: await db.countUnread(roomId, memberId) });
}));

app.post('/api/notify/inbox/read', requireLogin, wrap(async (req, res) => {
    const { roomId, memberId } = req.session;
    await db.markAllRead(roomId, memberId);
    ok(res);
}));

// ── 알림 설정 ───────────────────────────────────────────
app.get('/api/notify/settings', requireLogin, wrap(async (req, res) => {
    ok(res, { remindOn: await db.getRemindOn(req.session.memberId) });
}));

app.put('/api/notify/settings', requireLogin, wrap(async (req, res) => {
    const { remindOn } = req.body || {};
    if (typeof remindOn !== 'boolean') {
        return fail(res, 400, 'remindOn은 true 또는 false여야 합니다.');
    }
    await db.setRemindOn(req.session.memberId, remindOn);
    ok(res, { remindOn });
}));

// ── 헬스 체크 (docker exec cleanmate-notify curl -s localhost:3000/health) ──
app.get('/health', async (req, res) => {
    const result = { redis: 'DOWN', db: 'DOWN', stream: streamConsumer.status };
    try { await redis.client.ping(); result.redis = 'UP'; } catch (e) { /* DOWN */ }
    try { await db.ping(); result.db = 'UP'; } catch (e) { /* DOWN */ }
    const healthy = result.redis === 'UP' && result.db === 'UP';
    res.status(healthy ? 200 : 503).json({ success: healthy, result, message: null });
});

// ── 없는 경로와 오류 처리 ──────────────────────────────
app.use((req, res) => fail(res, 404, '찾을 수 없습니다.'));

app.use((err, req, res, next) => {
    // JSON 형식이 틀린 요청
    if (err.type === 'entity.parse.failed') return fail(res, 400, '요청 형식이 올바르지 않습니다.');
    // DB 연결 실패 등은 Java 서버와 같은 안내(503)
    if (['ECONNREFUSED', 'ETIMEDOUT', 'PROTOCOL_CONNECTION_LOST', 'ENOTFOUND', 'EAI_AGAIN'].includes(err.code)) {
        console.warn(`DB 연결 실패: ${err.message}`);
        return fail(res, 503, '잠시 후 다시 시도하세요.');
    }
    console.error(err);
    fail(res, 500, '서버 오류가 발생했습니다.');
});

// ── 시작 ───────────────────────────────────────────────
async function main() {
    try {
        await redis.connect();
    } catch (e) {
        // Redis 없이 시작하면 세션도 이벤트도 못 받으므로 종료한다.
        // 컨테이너는 --restart on-failure 라서 잠시 뒤 다시 시작된다.
        console.error(`Redis 연결 실패로 종료: ${e.message}`);
        process.exit(1);
    }
    await streamConsumer.start();
    reminder.start();
    app.listen(config.port, () => console.log(`cleanmate-notify listening on ${config.port}`));
}

main();
