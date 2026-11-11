/*
 * stats.js — 누적 기록 화면 (R2, 로그인 불필요)
 *
 * 사용하는 API
 *   GET /api/auth/me   상단 메뉴용
 *   GET /api/stats     { members: [{ memberId, name, assigned, selfDone, missed, substitute, rate }] }
 *                      rate: "75%" 같은 문자열, 담당이 0이면 null
 */

$(function () {
    Api.me()
        .then(function (me) {
            Page.renderHeader(me);
            return Api.get('/api/stats');
        })
        .then(function (stats) {
            const rows = stats.members.map(function (m) {
                return '<tr>'
                    + '<td class="name">' + Page.esc(m.name) + '</td>'
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
