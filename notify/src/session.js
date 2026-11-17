/*
 * session.js — Java 서버와 같은 로그인 세션을 읽는 미들웨어 (이기종 세션 공유)
 *
 * 인증 서버(Spring Boot)가 로그인할 때 Redis에 저장한 세션을 그대로 읽는다.
 *   쿠키:  CMSESSION=<UUID>  (HttpOnly, 프록시 덕분에 같은 주소라 그대로 전달됨)
 *   Redis: session:<UUID> = {"memberId":2,"roomId":1,"role":"MEMBER","name":"지현","mustChangePw":false}
 * 세션 형식을 언어와 무관한 JSON으로 정했기 때문에 Node.js는 JSON.parse만 하면 된다
 * (실습 10에서는 PHP 직렬화 형식을 Python이 흉내 내야 했음).
 * 세션을 만들고 지우는 것은 인증 서버만 한다. 알림 서버는 읽기만 한다.
 */
const redis = require('./redis');
const { fail } = require('./response');

const COOKIE_NAME = 'CMSESSION';
const KEY_PREFIX = 'session:';

/** Cookie 헤더 "a=1; CMSESSION=abc" 에서 CMSESSION 값을 꺼낸다 */
function readSessionId(req) {
    const header = req.headers.cookie || '';
    for (const part of header.split(';')) {
        const [name, ...rest] = part.trim().split('=');
        if (name === COOKIE_NAME) return rest.join('=');
    }
    return null;
}

/**
 * 로그인이 필요한 API 앞에 두는 미들웨어.
 * 세션이 있으면 req.session에 넣고 다음으로, 없으면 401.
 * 비밀번호 변경이 필요한 사용자는 조회(GET)만 허용하고 바꾸는 요청은 403 (Java 서버와 같은 규칙).
 */
async function requireLogin(req, res, next) {
    const sessionId = readSessionId(req);
    if (!sessionId) return fail(res, 401, '로그인이 필요합니다.');

    let json;
    try {
        json = await redis.client.get(KEY_PREFIX + sessionId);
    } catch (e) {
        return fail(res, 503, '잠시 후 다시 시도하세요.');
    }
    if (!json) return fail(res, 401, '로그인이 필요합니다.');

    let session;
    try {
        session = JSON.parse(json);
    } catch (e) {
        return fail(res, 401, '로그인이 필요합니다.');
    }

    if (session.mustChangePw === true && req.method !== 'GET') {
        return fail(res, 403, '비밀번호를 먼저 변경하세요.');
    }
    req.session = session;   // { memberId, roomId, role, name, mustChangePw }
    next();
}

module.exports = { requireLogin, readSessionId };
