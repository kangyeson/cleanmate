package com.example.cleanmate.global.session;

/**
 * Redis에 JSON으로 저장되는 세션 데이터.
 * key: session:{UUID}, TTL: 7일
 */
public record SessionData(Long memberId, Long roomId, String role, String name) {
}
