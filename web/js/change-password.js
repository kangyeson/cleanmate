/*
 * change-password.js — 비밀번호 변경 화면
 *
 * 사용하는 API
 *   GET /api/auth/me                                   로그인 확인
 *   PUT /api/auth/password { currentPassword, newPassword }
 *
 * mustChangePw=true인 사용자는 여기서 반드시 변경해야 다른 페이지를 이용할 수 있다.
 * 이 화면 자체는 mustChangePw redirect를 하지 않는다.
 */

$(function () {
    Api.me()
        .then(function (me) {
            if (!me) {
                location.href = 'login.html';
                return;
            }
            Page.renderHeader(me);
            if (me.mustChangePw) {
                Page.showMessage('임시 비밀번호를 사용 중입니다. 새 비밀번호로 변경해 주세요.', 'info');
            }
        })
        .catch(function () {
            location.href = 'login.html';
        });

    $('#change-form').on('submit', function (e) {
        e.preventDefault();
        Page.clearMessage();

        const currentPassword = $('#current-password').val();
        const newPassword = $('#new-password').val();
        const confirm = $('#new-password-confirm').val();

        if (newPassword !== confirm) {
            Page.showMessage('새 비밀번호가 일치하지 않습니다.');
            return;
        }

        const $btn = $(this).find('button[type=submit]').prop('disabled', true);
        Api.put('/api/auth/password', { currentPassword: currentPassword, newPassword: newPassword })
            .then(function () {
                location.href = 'index.html';
            })
            .catch(function (err) {
                $btn.prop('disabled', false);
                Page.showMessage(err.message);
                $('#current-password').val('').trigger('focus');
            });
    });
});
