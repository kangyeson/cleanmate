package com.example.cleanmate.domain.room.controller;

import com.example.cleanmate.domain.client.DutyClient;
import com.example.cleanmate.domain.room.dto.CreateRoomRequest;
import com.example.cleanmate.domain.room.service.RoomService;
import com.example.cleanmate.global.apiPayload.ApiResponse;
import com.example.cleanmate.global.session.SessionData;
import com.example.cleanmate.global.session.SessionStore;
import com.example.cleanmate.global.web.SessionCookie;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/rooms")
public class RoomController {

    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(RoomController.class);

    private final RoomService roomService;
    private final SessionStore sessionStore;
    private final DutyClient dutyClient;

    public RoomController(RoomService roomService, SessionStore sessionStore, DutyClient dutyClient) {
        this.roomService = roomService;
        this.sessionStore = sessionStore;
        this.dutyClient = dutyClient;
    }

    /**
     * 방 생성.
     * DB 커밋 후 세션을 생성해 관리자 CMSESSION 쿠키를 발급한다.
     */
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<Void> createRoom(
            @Valid @RequestBody CreateRoomRequest request,
            HttpServletResponse response) {
        SessionData sessionData = roomService.createRoom(request);  // @Transactional → 여기서 커밋
        String sessionId = sessionStore.create(sessionData);        // 커밋 후 Redis 세션 생성
        SessionCookie.addTo(response, sessionId, SessionCookie.MAX_AGE);
        // 첫 주 배정 생성 (best-effort: 실패해도 방 생성은 성공)
        try {
            dutyClient.ensureCurrentWeek(sessionData.roomId());
        } catch (Exception e) {
            log.warn("방 생성 후 배정 초기화 실패 (무시): {}", e.getMessage());
        }
        return ApiResponse.ok();
    }

    /** 방 종료 (관리자 전용). */
    @PostMapping("/close")
    public ApiResponse<Void> closeRoom(
            @CookieValue(name = "CMSESSION", required = false) String sessionId) {
        roomService.closeRoom(sessionId);
        return ApiResponse.ok();
    }
}
