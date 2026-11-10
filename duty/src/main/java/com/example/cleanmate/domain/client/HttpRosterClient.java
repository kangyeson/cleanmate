package com.example.cleanmate.domain.client;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

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
        try {
            RosterApiResponse response = restClient.get()
                    .uri("/internal/roster")
                    .retrieve()
                    .onStatus(HttpStatusCode::is4xxClientError, (req, res) -> {
                        if (res.getStatusCode().value() == 404) {
                            throw new NoActiveRoomException();
                        }
                        throw new RosterUnavailableException();
                    })
                    .body(RosterApiResponse.class);

            if (response == null || !response.success()) {
                throw new RosterUnavailableException();
            }
            return response.result();
        } catch (NoActiveRoomException | RosterUnavailableException e) {
            throw e;
        } catch (RestClientException e) {
            throw new RosterUnavailableException();
        }
    }
}
