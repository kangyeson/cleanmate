/*
 * login.js — 로그인 화면 (R3)
 *
 * 사용하는 API
 *   GET  /api/auth/me     이미 로그인했는지 확인
 *   POST /api/auth/login  { name, password } → 성공 시 서버가 CMSESSION 쿠키(HttpOnly)를 내려줌
 *
 * 실패 횟수 제한은 없다(요구사항). 틀리면 서버 안내 문구를 그대로 보여 준다.
 */

$(function () {
    Api.me()
        .then(function (me) {
            Page.renderHeader(me);
            if (me) {
                Page.showMessage(me.name + '(으)로 로그인되어 있습니다.', 'info');
            }
        })
        .catch(function () {
            Page.renderHeader(null);
        });

    $('#login-form').on('submit', function (e) {
        e.preventDefault();                       // 폼 기본 제출(페이지 이동) 막기
        Page.clearMessage();

        const name = $('#name').val().trim();
        const password = $('#password').val();
        if (!name || !password) {
            Page.showMessage('이름과 비밀번호를 입력하세요.');
            return;
        }

        const $btn = $(this).find('button[type=submit]').prop('disabled', true);
        Api.post('/api/auth/login', { name: name, password: password })
            .then(function () {
                location.href = 'index.html';    // 로그인 성공 → 이번 주 화면
            })
            .catch(function (err) {
                $btn.prop('disabled', false);
                Page.showMessage(err.message);
                $('#password').val('').trigger('focus');
            });
    });
});
