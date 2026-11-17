/*
 * reminder.js — 미완료 리마인더 (수·토 20시, Asia/Seoul)
 *
 * 1) 인증 서버 GET /internal/roster  → 진행 중인 방 id와 룸메이트(활성 여부)
 * 2) 당번 서버 GET /api/duty/week     → 이번 주 구역별 담당과 상태 (duty_pool 별칭으로 2대 중 한 대)
 * 3) 아직 PENDING인 구역의 담당자 중 리마인더를 끄지 않은 사람에게 알림함 항목을 만든다.
 *
 * 다른 서버의 데이터는 그 서버의 API로만 얻는다(알림 서버는 cleanmate_notify 스키마에만 권한이 있음).
 * 같은 날 같은 사람·구역 리마인더는 event_key로 한 번만 저장된다(작업이 다시 돌아도 안전).
 */
const cron = require('node-cron');
const config = require('./config');
const db = require('./db');
const { reminderMessage } = require('./messages');

/** 다른 서버의 API를 불러 result를 꺼낸다. 3초 안에 답이 없거나 실패하면 예외 */
async function getJson(url) {
    const res = await fetch(url, { signal: AbortSignal.timeout(3000) });
    if (!res.ok) throw new Error(`${url} → ${res.status}`);
    const body = await res.json();
    return body.result;
}

/** 오늘 날짜 "2026-11-18" (서울 기준) */
function todaySeoul() {
    return new Intl.DateTimeFormat('sv-SE', { timeZone: config.timezone }).format(new Date());
}

async function runReminder() {
    try {
        const roster = await getJson(`${config.authUrl}/internal/roster`);
        const week = await getJson(`${config.dutyUrl}/api/duty/week`);
        const activeIds = new Set(roster.members.filter(m => m.active).map(m => Number(m.id)));
        const offIds = await db.getRemindOffMemberIds();
        const today = todaySeoul();

        let created = 0;
        for (const d of week.duties) {
            const memberId = Number(d.assigneeId);
            if (d.status !== 'PENDING') continue;          // 이미 완료된 구역
            if (!activeIds.has(memberId)) continue;        // 삭제된 룸메이트
            if (offIds.has(memberId)) continue;            // 리마인더를 끈 사람
            const saved = await db.insertNotification({
                roomId: Number(roster.roomId),
                memberId,
                type: 'REMINDER',
                message: reminderMessage(d.zoneName),
                eventKey: `REMIND:${today}:${memberId}:${d.zoneId}`,
            });
            if (saved) created++;
        }
        console.log(`[reminder] ${today} 리마인더 ${created}건`);
    } catch (e) {
        // 진행 중인 방이 없거나(404) 다른 서버가 멈춘 경우: 이번 회차는 건너뛰고 로그만 남김
        console.error(`[reminder] 건너뜀: ${e.message}`);
    }
}

function start() {
    cron.schedule(config.remindCron, runReminder, { timezone: config.timezone });
    console.log(`[reminder] 일정 "${config.remindCron}" (${config.timezone})`);
}

module.exports = { start, runReminder };
