package com.example.cleanmate.global.session;

import java.util.Optional;

/**
 * duty 서버는 세션을 읽기만 함. 생성·삭제는 auth 서버 담당.
 */
public interface SessionStore {
    Optional<SessionData> find(String sessionId);
}
