/*
 * messages.js — 알림 문구 만들기 (DB·Redis 없이 시험할 수 있게 따로 둠)
 */

/**
 * 완료 이벤트 → 알림 문구
 *   본인 완료:  "지현 – 화장실 완료 (1/4)"
 *   대신 청소:  "민정 – 세면대 대신 청소 (서현 담당, 2/4)"
 * @param {object} e 스트림 필드 (값은 모두 문자열)
 */
function doneMessage(e) {
    const progress = `${e.doneCount}/${e.total}`;
    if (e.substitute === 'true') {
        return `${e.doneByName} – ${e.zoneName} 대신 청소 (${e.assigneeName} 담당, ${progress})`;
    }
    return `${e.doneByName} – ${e.zoneName} 완료 (${progress})`;
}

/** 미완료 리마인더 문구 */
function reminderMessage(zoneName) {
    return `이번 주 ${zoneName} 청소가 아직 기록되지 않았어요.`;
}

module.exports = { doneMessage, reminderMessage };
