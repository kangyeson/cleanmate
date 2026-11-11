/*
 * history.js — 주별 기록 화면 (로그인 불필요)
 *
 * 사용하는 API
 *   GET /api/auth/me   상단 메뉴용 (없으면 null)
 *   GET /api/history   { weeks: [{ weekStart, duties: [{ zoneId, zoneName, assigneeId,
 *                         assigneeName, status, doneById, doneByName, doneDate }] }] }
 *                      weeks는 최신 주 순으로 정렬됨
 */

$(function () {
    Api.me()
        .then(function (me) {
            Page.renderHeader(me);
            if (Page.guardMustChangePw(me)) return;
        })
        .catch(function () {
            Page.renderHeader(null);
        });

    Api.get('/api/history')
        .then(function (history) {
            if (!history.weeks || history.weeks.length === 0) {
                Page.showMessage('아직 기록된 주가 없습니다.', 'info');
                return;
            }
            renderHistory(history);
            $('#history-section').prop('hidden', false);
        })
        .catch(function (err) {
            if (err.status === 404) {
                $('#no-room').prop('hidden', false);
                return;
            }
            Page.showMessage(err.message);
        });
});

function renderHistory(history) {
    const html = history.weeks.map(function (week) {
        const monday = new Date(week.weekStart + 'T00:00:00');
        const sunday = new Date(monday);
        sunday.setDate(monday.getDate() + 6);
        const period = (monday.getMonth() + 1) + '/' + monday.getDate()
            + ' ~ ' + (sunday.getMonth() + 1) + '/' + sunday.getDate();

        const doneCount = week.duties.filter(function (d) { return d.status === 'DONE'; }).length;
        const rows = week.duties.map(function (d) { return dutyRow(d); }).join('');

        return '<div class="card">'
            + '<div class="card-row">'
            + '  <strong>' + period + '</strong>'
            + '  <span class="week-score">' + doneCount + ' / ' + week.duties.length + ' 완료</span>'
            + '</div>'
            + '<table class="week-table">'
            + '  <thead><tr><th>구역</th><th>담당</th><th>결과</th></tr></thead>'
            + '  <tbody>' + rows + '</tbody>'
            + '</table>'
            + '</div>';
    }).join('');

    $('#history-list').html(html);
}

function dutyRow(d) {
    let resultText;
    if (d.status === 'DONE') {
        if (d.doneById !== d.assigneeId) {
            resultText = '완료 (' + d.doneByName + ' 대신)';
        } else {
            resultText = '완료';
        }
        if (d.doneDate) resultText += ' ' + Page.shortDate(d.doneDate);
    } else if (d.status === 'MISSED') {
        resultText = '미완료 확정';
    } else {
        resultText = '미완료';
    }

    return '<tr>'
        + '<td class="name">' + Page.esc(d.zoneName) + '</td>'
        + '<td>' + Page.esc(d.assigneeName) + '</td>'
        + '<td><span class="status ' + d.status + '">' + Page.esc(resultText) + '</span></td>'
        + '</tr>';
}
