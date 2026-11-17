// 알림 문구 시험 (node --test test/)
const test = require('node:test');
const assert = require('node:assert');
const { doneMessage, reminderMessage } = require('../src/messages');

test('본인 완료 문구', () => {
    const e = { doneByName: '지현', zoneName: '화장실', substitute: 'false', doneCount: '1', total: '4' };
    assert.strictEqual(doneMessage(e), '지현 – 화장실 완료 (1/4)');
});

test('대신 청소 문구에는 원래 담당자가 들어간다', () => {
    const e = { doneByName: '민정', zoneName: '세면대', assigneeName: '서현', substitute: 'true', doneCount: '2', total: '4' };
    assert.strictEqual(doneMessage(e), '민정 – 세면대 대신 청소 (서현 담당, 2/4)');
});

test('리마인더 문구', () => {
    assert.strictEqual(reminderMessage('바닥'), '이번 주 바닥 청소가 아직 기록되지 않았어요.');
});
