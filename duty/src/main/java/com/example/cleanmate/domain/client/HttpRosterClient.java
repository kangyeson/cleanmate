package com.example.cleanmate.domain.client;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

/**
 * auth 서버의 GET /internal/roster를 호출해 명단을 가져온다.
 * StubRosterClient를 대체한다.
 *
 * 환경 변수 AUTH_URL로 auth 서버 주소 지정 (기본값 http://localhost:8081).
 */
@Component
public class HttpRosterClient implements RosterClient {

    /** ApiResponse<RosterResponse> 역직렬화용 내부 레코드 */
    private record RosterApiResponse(boolean success, RosterResponse result, String message) {}

    private final RestClient restClient;

    public HttpRosterClient(RestClient.Builder builder,
                            @Value("${roster.auth-url}") String authUrl) {
        this.restClient = builder.baseUrl(authUrl).build();
    }

    @Override
    public RosterResponse getRoster() {
        RosterApiResponse response = restClient.get()
                .uri("/internal/roster")
                .retrieve()
                .body(RosterApiResponse.class);

        if (response == null || !response.success()) {
            throw new RuntimeException("명단 조회 실패: auth 서버 응답 없음");
        }
        return response.result();
    }
}
