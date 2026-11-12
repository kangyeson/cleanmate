/*
 * stats.js — 누적 기록 화면 (R2, 로그인 불필요)
 *
 * 사용하는 API
 *   GET /api/auth/me   상단 메뉴용
 *   GET /api/stats     { confirmedWeeks, members: [{ memberId, name, active, assigned, selfDone, missed, substitute, rate }] }
 *                      rate: "75%" 같은 문자열, 담당이 0이면 null
 */

$(function () {
    Api.me()
        .then(function (me) {
            Page.renderHeader(me);
            return Api.get('/api/stats');
        })
        .then(function (stats) {
            if (stats.confirmedWeeks === 0) {
                $('#stats-empty').prop('hidden', false);
                $('#stats-section').prop('hidden', true);
                return;
            }
            const rows = stats.members.map(function (m) {
                const displayName = m.active
                    ? Page.esc(m.name)
                    : Page.esc(m.name) + ' <span class="hint">(퇴실)</span>';
                return '<tr>'
                    + '<td class="name">' + displayName + '</td>'
                    + '<td>' + m.assigned + '</td>'
                    + '<td>' + m.selfDone + '</td>'
                    + '<td>' + m.missed + '</td>'
                    + '<td>' + m.substitute + '</td>'
                    + '<td>' + (m.rate == null ? '-' : Page.esc(m.rate)) + '</td>'
                    + '</tr>';
            });
            $('#stats-body').html(rows.join(''));
            $('#stats-section').prop('hidden', false);
        })
        .catch(function (err) {
            if (err.status === 404) {
                $('#no-room').prop('hidden', false);
                return;
            }
            Page.showMessage(err.message);
        });
});
