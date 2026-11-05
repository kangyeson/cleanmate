package com.example.cleanmate.global.session;

import java.util.Optional;

public interface SessionStore {
    /** 세션 생성 → UUID 반환 */
    String create(SessionData data);

    /** UUID로 세션 조회 */
    Optional<SessionData> find(String sessionId);

    /** 세션 삭제 (로그아웃) */
    void delete(String sessionId);
}
