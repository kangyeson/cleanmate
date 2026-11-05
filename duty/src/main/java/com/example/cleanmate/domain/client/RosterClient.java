package com.example.cleanmate.domain.client;

/**
 * 인증 서버에서 현재 방의 명단을 가져오는 클라이언트.
 * 개발 단계에서는 StubRosterClient, 인증 서버 완성 후 HttpRosterClient로 교체한다.
 */
public interface RosterClient {
    RosterResponse getRoster();
}
