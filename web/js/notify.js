/*
 * notify.js — 알림 화면 (알림함 + 알림 설정)
 *
 * 이 화면의 API는 모두 알림 서버(Node.js Express, cleanmate-notify)가 처리한다.
 * 로그인 상태는 Java 인증 서버가 만든 같은 세션(CMSESSION)으로 인식된다.
 *
 * 사용하는 API
 *   GET  /api/auth/me              로그인 확인 (인증 서버)
 *   GET  /api/notify/inbox         { unread, items: [{ id, type: DONE|REMINDER, message, createdAt, unread }] }
 *   POST /api/notify/inbox/read    지금까지의 알림을 모두 읽음으로 표시
 *   GET  /api/notify/settings      { remindOn }
 *   PUT  /api/notify/settings      { remindOn: true|false }
 */

$(function () {
    Api.me()
        .then(function (me) {
            Page.renderHeader(me);
            if (!me) {                       // 알림함은 로그인한 사람만
                location.href = 'login.html';
                return;
            }
            if (Page.guardMustChangePw(me)) return;
            loadSettings();
            loadInbox();
        })
        .catch(function (err) {
            Page.renderHeader(null);
            Page.showMessage(err.message);
        });

    // 리마인더 받기 켜기·끄기
    $('#remind-on').on('change', function () {
        const $box = $(this);
        const remindOn = $box.is(':checked');
        $box.prop('disabled', true);
        Api.put('/api/notify/settings', { remindOn: remindOn })
            .then(function () {
                Page.showMessage(remindOn ? '리마인더를 받습니다.' : '리마인더를 받지 않습니다.', 'info');
            })
            .catch(function (err) {
                $box.prop('checked', !remindOn);   // 실패하면 원래대로
                Page.showMessage(err.message);
            })
            .finally(function () { $box.prop('disabled', false); });
    });
});

function loadSettings() {
    Api.get('/api/notify/settings')
        .then(function (s) {
            $('#remind-on').prop('checked', s.remindOn).prop('disabled', false);
        })
        .catch(function (err) { Page.showMessage(err.message); });
}

/** 알림함을 그리고, 다 그린 뒤 모두 읽음으로 표시한다 (상단 숫자도 0으로) */
function loadInbox() {
    Api.get('/api/notify/inbox')
        .then(function (inbox) {
            renderInbox(inbox.items);
            if (inbox.unread > 0) {
                return Api.post('/api/notify/inbox/read').then(Page.refreshNotifyCount);
            }
        })
        .catch(function (err) {
            // 알림 서버가 멈춰 있으면 이 화면만 안내하고, 다른 화면은 그대로 쓸 수 있다
            Page.showMessage(err.message);
        });
}

function renderInbox(items) {
    if (!items || items.length === 0) {
        $('#notify-empty').prop('hidden', false);
        $('#notify-list').empty();
        return;
    }
    const html = items.map(function (n) {
        const kind = n.type === 'REMINDER' ? '리마인더' : '완료';
        return '<li class="' + (n.unread ? 'unread' : '') + '">'
            + '<span class="kind ' + Page.esc(n.type) + '">' + kind + '</span>'
            + Page.esc(n.message)
            + '<span class="time">' + Page.esc(n.createdAt) + '</span>'
            + '</li>';
    });
    $('#notify-empty').prop('hidden', true);
    $('#notify-list').html(html.join(''));
}
