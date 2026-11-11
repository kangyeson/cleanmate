package com.example.cleanmate.global.session;

/**
 * Redis 세션에서 읽은 데이터. auth 서버와 JSON 필드명 동일.
 * key: session:{UUID}
 *
 * mustChangePw: 기존 세션(이 필드 없음)을 Jackson이 역직렬화할 때 null이 되므로 Boolean(nullable) 사용.
 */
public record SessionData(Long memberId, Long roomId, String role, String name, Boolean mustChangePw) {
}
