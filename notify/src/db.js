/*
 * db.js — MySQL 연결(풀)과 알림 서버가 쓰는 쿼리
 *
 * 테이블 (db/init/01-schema.sql, 스키마 cleanmate_notify)
 *   notify_settings(member_id, remind_on)          회원별 리마인더 받기 설정
 *   notifications(id, room_id, member_id, type, message, event_key, created_at)
 *       member_id가 NULL이면 방 전체 알림(완료 소식), 값이 있으면 그 회원에게만(리마인더)
 *       event_key UNIQUE: 같은 알림이 두 번 저장되지 않게 함
 *   notify_reads(member_id, last_read_id)          어디까지 읽었는지
 */
const mysql = require('mysql2/promise');
const config = require('./config');

// 연결 풀: 요청마다 연결을 새로 만들지 않고 재사용한다.
// connectTimeout을 짧게 두어 DB가 멈췄을 때 오래 기다리지 않고 503으로 답한다.
const pool = mysql.createPool({
    ...config.db,
    waitForConnections: true,
    connectionLimit: 5,
    connectTimeout: 3000,
    timezone: '+09:00',
    dateStrings: true,      // DATETIME을 "2026-11-18 20:00:00" 문자열로 받음
});

/** 알림 한 건 저장. 같은 event_key가 이미 있으면 무시(INSERT IGNORE)하고 false를 돌려준다. */
async function insertNotification({ roomId, memberId = null, type, message, eventKey }) {
    const [result] = await pool.execute(
        `INSERT IGNORE INTO notifications (room_id, member_id, type, message, event_key)
         VALUES (?, ?, ?, ?, ?)`,
        [roomId, memberId, type, message, eventKey]
    );
    return result.affectedRows === 1;
}

/** 리마인더 받기 설정. 행이 없으면 기본값 true. */
async function getRemindOn(memberId) {
    const [rows] = await pool.execute(
        'SELECT remind_on FROM notify_settings WHERE member_id = ?', [memberId]);
    return rows.length === 0 ? true : rows[0].remind_on === 1;
}

/** 리마인더 받기 설정 저장 (없으면 추가, 있으면 수정) */
async function setRemindOn(memberId, remindOn) {
    await pool.execute(
        `INSERT INTO notify_settings (member_id, remind_on) VALUES (?, ?)
         ON DUPLICATE KEY UPDATE remind_on = VALUES(remind_on)`,
        [memberId, remindOn]);
}

/** 리마인더를 끈 회원 id 목록 (리마인더 작업에서 한 번에 조회) */
async function getRemindOffMemberIds() {
    const [rows] = await pool.execute(
        'SELECT member_id FROM notify_settings WHERE remind_on = FALSE');
    return new Set(rows.map(r => Number(r.member_id)));
}

/** 내 알림함: 방 전체 알림 + 나에게 온 알림, 최신순 limit개 */
async function getInbox(roomId, memberId, limit = 30) {
    const [rows] = await pool.query(
        `SELECT id, type, message, created_at AS createdAt
           FROM notifications
          WHERE room_id = ? AND (member_id IS NULL OR member_id = ?)
          ORDER BY id DESC
          LIMIT ?`,
        [roomId, memberId, limit]);
    return rows;
}

/** 마지막으로 읽은 알림 id (없으면 0) */
async function getLastReadId(memberId) {
    const [rows] = await pool.execute(
        'SELECT last_read_id FROM notify_reads WHERE member_id = ?', [memberId]);
    return rows.length === 0 ? 0 : Number(rows[0].last_read_id);
}

/** 안 읽은 알림 수 (마지막으로 읽은 id보다 큰 내 알림 수) */
async function countUnread(roomId, memberId) {
    const lastReadId = await getLastReadId(memberId);
    const [rows] = await pool.execute(
        `SELECT COUNT(*) AS cnt FROM notifications
          WHERE room_id = ? AND (member_id IS NULL OR member_id = ?) AND id > ?`,
        [roomId, memberId, lastReadId]);
    return Number(rows[0].cnt);
}

/** 지금까지 온 알림을 모두 읽음으로 표시 */
async function markAllRead(roomId, memberId) {
    const [rows] = await pool.execute(
        `SELECT COALESCE(MAX(id), 0) AS maxId FROM notifications
          WHERE room_id = ? AND (member_id IS NULL OR member_id = ?)`,
        [roomId, memberId]);
    await pool.execute(
        `INSERT INTO notify_reads (member_id, last_read_id) VALUES (?, ?)
         ON DUPLICATE KEY UPDATE last_read_id = GREATEST(last_read_id, VALUES(last_read_id))`,
        [memberId, Number(rows[0].maxId)]);
}

/** 헬스 체크용 */
async function ping() {
    await pool.query('SELECT 1');
}

module.exports = {
    insertNotification, getRemindOn, setRemindOn, getRemindOffMemberIds,
    getInbox, countUnread, markAllRead, ping,
};
