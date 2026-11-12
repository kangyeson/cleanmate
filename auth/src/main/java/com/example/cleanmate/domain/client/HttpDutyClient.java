package com.example.cleanmate.domain.client;

import com.example.cleanmate.global.exception.CleanmateException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

@Component
public class HttpDutyClient implements DutyClient {

    private final RestClient restClient;

    public HttpDutyClient(@Value("${duty.url:http://localhost:8082}") String dutyUrl) {
        this.restClient = RestClient.builder().baseUrl(dutyUrl).build();
    }

    @Override
    public void finalizeRoom(Long roomId) {
        try {
            restClient.post()
                .uri("/internal/rooms/" + roomId + "/finalize")
                .retrieve()
                .toBodilessEntity();
        } catch (RestClientException e) {
            throw new CleanmateException("당번 서버와 통신 중 오류가 발생했습니다.", 503);
        }
    }

    @Override
    public void ensureCurrentWeek(Long roomId) {
        try {
            restClient.post()
                .uri("/internal/rooms/" + roomId + "/current-week")
                .retrieve()
                .toBodilessEntity();
        } catch (RestClientException e) {
            throw new CleanmateException("당번 서버와 통신 중 오류가 발생했습니다.", 503);
        }
    }
}
