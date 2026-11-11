package com.example.cleanmate.global.session;

/**
 * Redis에 JSON으로 저장되는 세션 데이터.
 * key: session:{UUID}, TTL: 7일
 *
 * mustChangePw: 기존 세션(이 필드 없음)을 Jackson이 역직렬화할 때 null이 되므로 Boolean(nullable) 사용.
 */
public record SessionData(Long memberId, Long roomId, String role, String name, Boolean mustChangePw) {
}
