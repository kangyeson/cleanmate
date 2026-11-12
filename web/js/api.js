/*
 * api.js — 모든 화면이 함께 쓰는 공통 코드
 *
 * 1) Api: 서버 API 호출
 *    - 주소는 항상 "/api/..." 상대 경로로 쓴다.
 *      화면(Apache)과 두 서버(auth, duty)가 프록시(NPM) 한 주소로 묶여 있어서
 *      같은 주소로 보내면 CMSESSION 쿠키가 자동으로 함께 전송된다. (CORS 설정 불필요)
 *    - 서버 응답 형식: { success: true/false, result: ..., message: "..." }
 *      성공이면 result만 돌려주고, 실패면 { status, message } 로 reject 한다.
 *
 * 2) Page: 화면 공통 기능
 *    - 상단 메뉴(로그인 상태 표시), 메시지 표시, HTML 이스케이프
 */

const Api = (function () {

    /** HTTP 상태 코드별 기본 안내 (서버가 message를 주지 않았을 때) */
    function defaultMessage(status) {
        if (status === 0) return '서버에 연결할 수 없습니다. 잠시 후 다시 시도하세요.';
        if (status === 401) return '로그인이 필요합니다.';
        if (status === 403) return '권한이 없습니다.';
        if (status === 404) return '찾을 수 없습니다.';
        if (status >= 500) return '서버 오류가 발생했습니다. 잠시 후 다시 시도하세요.';
        return '요청을 처리하지 못했습니다.';
    }

    /**
     * 공통 요청 함수.
     * @param {string} method GET / POST / PUT / DELETE
     * @param {string} url    "/api/..." 상대 경로
     * @param {object} [body] JSON으로 보낼 데이터 (없으면 본문 없음)
     * @returns {Promise<any>} 성공 시 result
     */
    function request(method, url, body) {
        return new Promise(function (resolve, reject) {
            $.ajax({
                method: method,
                url: url,
                contentType: body ? 'application/json' : undefined,
                data: body ? JSON.stringify(body) : undefined,
                dataType: 'json'
            })
                .done(function (res) {
                    resolve(res ? res.result : null);
                })
                .fail(function (xhr) {
                    const res = xhr.responseJSON;
                    reject({
                        status: xhr.status,
                        message: (res && res.message) || defaultMessage(xhr.status)
                    });
                });
        });
    }

    return {
        get: function (url) { return request('GET', url); },
        post: function (url, body) { return request('POST', url, body); },
        put: function (url, body) { return request('PUT', url, body); },
        del: function (url) { return request('DELETE', url); },

        /**
         * 현재 로그인한 사용자. 로그인하지 않았으면 null.
         * CMSESSION 쿠키는 HttpOnly라 JS가 직접 읽을 수 없어서 서버(/api/auth/me)에 물어본다.
         */
        me: function () {
            return request('GET', '/api/auth/me').catch(function (err) {
                if (err.status === 401) return null;
                throw err;
            });
        }
    };
})();

const Page = (function () {

    /** 사용자 입력(이름 등)을 화면에 넣기 전에 HTML 특수문자를 바꾼다. (스크립트 삽입 방지) */
    function esc(text) {
        return String(text == null ? '' : text)
            .replace(/&/g, '&amp;')
            .replace(/</g, '&lt;')
            .replace(/>/g, '&gt;')
            .replace(/"/g, '&quot;')
            .replace(/'/g, '&#39;');
    }

    /** "2026-11-10" → "11/10" */
    function shortDate(isoDate) {
        if (!isoDate) return '';
        const parts = isoDate.split('-');
        return Number(parts[1]) + '/' + Number(parts[2]);
    }

    /** 메시지 영역(#message)에 안내 표시. type: 'error' | 'info' */
    function showMessage(text, type) {
        $('#message')
            .removeClass('error info')
            .addClass(type || 'error')
            .text(text)
            .show();
    }

    function clearMessage() {
        $('#message').hide().text('');
    }

    /**
     * 상단 메뉴를 그린다.
     * @param {object|null} me /api/auth/me 결과
     */
    function renderHeader(me) {
        let right;
        if (me) {
            const badge = me.role === 'ADMIN' ? ' <span class="badge">관리자</span>' : '';
            const pwLink = me.mustChangePw
                ? '<a href="change-password.html" class="link-btn pw-required">비밀번호 변경 필요</a>'
                : '<a href="change-password.html" class="link-btn">비밀번호 변경</a>';
            right = '<span class="who">' + esc(me.name) + badge + '</span>'
                + pwLink
                + '<button type="button" id="logout-btn" class="link-btn">로그아웃</button>';
        } else {
            right = '<a href="login.html">로그인</a>';
        }

        const adminLink = (me && me.role === 'ADMIN')
            ? '<a href="admin.html">관리</a>'
            : '';

        $('#header').html(
            '<div class="header-inner">'
            + '  <a class="brand" href="index.html">청소 당번 도우미</a>'
            + '  <nav>'
            + '    <a href="index.html">이번 주</a>'
            + '    <a href="stats.html">누적 기록</a>'
            + '    <a href="history.html">주별 기록</a>'
            + (adminLink ? '    ' + adminLink : '')
            + '  </nav>'
            + '  <div class="user">' + right + '</div>'
            + '</div>'
        );

        $('#logout-btn').on('click', function () {
            Api.post('/api/auth/logout')
                .finally(function () { location.href = 'index.html'; });
        });
    }

    /**
     * mustChangePw=true인 로그인 사용자를 비밀번호 변경 화면으로 보낸다.
     * @returns {boolean} true면 redirect 발생 — 호출부에서 이후 코드를 중단해야 한다.
     */
    function guardMustChangePw(me) {
        if (me && me.mustChangePw) {
            location.href = 'change-password.html';
            return true;
        }
        return false;
    }

    return {
        esc: esc,
        shortDate: shortDate,
        showMessage: showMessage,
        clearMessage: clearMessage,
        renderHeader: renderHeader,
        guardMustChangePw: guardMustChangePw
    };
})();
