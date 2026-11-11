/*
 * index.js — 이번 주 현황 화면
 *
 * 사용하는 API
 *   GET    /api/auth/me                 로그인한 사람 (없으면 null)
 *   GET    /api/duty/week               이번 주 구역별 담당·완료 여부 (로그인 불필요)
 *   GET    /api/duty/mine               내 담당 구역 (로그인 필요)
 *   POST   /api/duty/{zoneId}/done      본인 구역 완료
 *   POST   /api/duty/{zoneId}/substitute 다른 사람 구역 대신 청소
 *   DELETE /api/duty/{zoneId}/record    내가 남긴 기록 취소
 *
 * 버튼은 화면에서 "보여 줄지"만 정한다. 실제 권한 판단은 서버(서비스 계층)가 한다.
 */

$(function () {
    load();
});

/**
 * 화면 전체를 다시 불러온다. 기록을 바꾼 뒤에도 이 함수를 다시 부른다.
 * @param {boolean} [keepMessage] true면 방금 보여 준 오류 안내를 지우지 않는다.
 */
function load(keepMessage) {
    if (!keepMessage) Page.clearMessage();

    Api.me()
        .then(function (me) {
            Page.renderHeader(me);
            return Api.get('/api/duty/week').then(function (week) {
                renderWeek(week, me);
                if (me) {
                    return Api.get('/api/duty/mine').then(function (mine) {
                        renderMine(mine, me);
                    });
                }
            });
        })
        .catch(function (err) {
            if (err.status === 404) {
                // 진행 중인 방이 없음 → 방 만들기 안내
                $('#week-section, #mine-section').prop('hidden', true);
                $('#no-room').prop('hidden', false);
                return;
            }
            Page.showMessage(err.message);
        });
}

/** 상태를 사람이 읽는 문구로 바꾼다. */
function statusText(item) {
    if (item.status === 'DONE') {
        // 청소한 사람이 담당자와 다르면 대신 청소
        if (item.doneById !== item.assigneeId) {
            return '완료 (' + Page.shortDate(item.doneDate) + ', 대신 청소: ' + item.doneByName + ')';
        }
        return '완료 (' + Page.shortDate(item.doneDate) + ')';
    }
    if (item.status === 'MISSED') return '미완료 확정';
    return '미완료';
}

/**
 * 구역 하나에 보여 줄 버튼을 고른다.
 *  - 본인 담당 + 미완료      → 완료
 *  - 다른 사람 담당 + 미완료 → 대신 청소
 *  - 내가 남긴 완료 기록     → 취소
 */
function actionButtons(item, me) {
    if (!me) return '';
    if (item.status === 'PENDING' && item.assigneeId === me.memberId) {
        return '<button type="button" data-action="done" data-zone="' + item.zoneId + '">완료</button>';
    }
    if (item.status === 'PENDING') {
        return '<button type="button" class="secondary" data-action="substitute" data-zone="' + item.zoneId + '"'
            + ' data-name="' + Page.esc(item.zoneName) + '">대신 청소</button>';
    }
    if (item.status === 'DONE' && item.doneById === me.memberId) {
        return '<button type="button" class="secondary" data-action="cancel" data-zone="' + item.zoneId + '"'
            + ' data-name="' + Page.esc(item.zoneName) + '">기록 취소</button>';
    }
    return '';
}

/** 구역 카드 하나 */
function dutyCard(item, me, highlightMine) {
    const isMine = me && item.assigneeId === me.memberId;
    return '<div class="card' + (highlightMine && isMine ? ' mine' : '') + '">'
        + '  <div class="card-row">'
        + '    <span class="zone">' + Page.esc(item.zoneName) + '</span>'
        + '    <span class="status ' + item.status + '">' + Page.esc(statusText(item)) + '</span>'
        + '  </div>'
        + '  <div class="sub">담당: ' + Page.esc(item.assigneeName) + '</div>'
        + '  <div class="actions">' + actionButtons(item, me) + '</div>'
        + '</div>';
}

function renderWeek(week, me) {
    const duties = week.duties;
    const doneCount = duties.filter(function (d) { return d.status === 'DONE'; }).length;

    // 주 기간: 월요일 ~ 일요일
    const monday = new Date(week.weekStart + 'T00:00:00');
    const sunday = new Date(monday);
    sunday.setDate(monday.getDate() + 6);
    const period = (monday.getMonth() + 1) + '/' + monday.getDate()
        + ' ~ ' + (sunday.getMonth() + 1) + '/' + sunday.getDate();

    $('#no-room').prop('hidden', true);
    $('#week-section').prop('hidden', false);
    $('#week-title').text('이번 주 청소 (' + period + ')');
    $('#week-summary').text('완료 ' + doneCount + ' / ' + duties.length);
    $('#week-list').html(duties.map(function (d) { return dutyCard(d, me, true); }).join(''));
    $('#login-hint').prop('hidden', !!me);
}

function renderMine(mine, me) {
    $('#mine-section').prop('hidden', false);
    if (mine.length === 0) {
        $('#mine-list').html('<p class="hint">이번 주에는 담당 구역이 없습니다.</p>');
        return;
    }
    $('#mine-list').html(mine.map(function (d) { return dutyCard(d, me, false); }).join(''));
}

/* 버튼 클릭 처리 (현황·내 담당 양쪽 카드 공통) */
$(document).on('click', 'button[data-action]', function () {
    const $btn = $(this);
    const action = $btn.data('action');
    const zoneId = $btn.data('zone');
    const zoneName = $btn.data('name');

    let call;
    if (action === 'done') {
        call = Api.post('/api/duty/' + zoneId + '/done');
    } else if (action === 'substitute') {
        if (!confirm(zoneName + ' 구역을 대신 청소한 것으로 기록할까요?')) return;
        call = Api.post('/api/duty/' + zoneId + '/substitute');
    } else if (action === 'cancel') {
        if (!confirm(zoneName + ' 구역의 내 기록을 취소할까요?')) return;
        call = Api.del('/api/duty/' + zoneId + '/record');
    } else {
        return;
    }

    $btn.prop('disabled', true);
    call
        .then(function () { load(); })
        .catch(function (err) {
            $btn.prop('disabled', false);
            if (err.status === 401) {
                location.href = 'login.html';
                return;
            }
            Page.showMessage(err.message);
            load(true);   // 다른 사람이 먼저 기록했을 수 있으니(409) 최신 상태로 다시 그림
        });
});
