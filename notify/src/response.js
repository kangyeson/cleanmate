/*
 * response.js — 응답 형식
 *
 * Java 서버(ApiResponse)와 같은 모양으로 돌려준다. 화면의 api.js가 서버 종류와 상관없이
 * 같은 방식으로 결과(result)와 안내 문구(message)를 꺼낼 수 있게 하기 위해서다.
 *   성공: { "success": true,  "result": ..., "message": null }
 *   실패: { "success": false, "result": null, "message": "로그인이 필요합니다." }
 */

function ok(res, result = null) {
    res.json({ success: true, result, message: null });
}

function fail(res, status, message) {
    res.status(status).json({ success: false, result: null, message });
}

module.exports = { ok, fail };
