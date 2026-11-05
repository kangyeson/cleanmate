package com.example.cleanmate.global.session;

/**
 * Redis 세션에서 읽은 데이터. auth 서버와 JSON 필드명 동일.
 * key: session:{UUID}
 */
public record SessionData(Long memberId, Long roomId, String role, String name) {
}
