package com.example.cleanmate.domain.room.controller;

import com.example.cleanmate.domain.room.dto.CreateRoomRequest;
import com.example.cleanmate.domain.room.service.RoomService;
import com.example.cleanmate.global.apiPayload.ApiResponse;
import com.example.cleanmate.global.session.SessionData;
import com.example.cleanmate.global.session.SessionStore;
import com.example.cleanmate.global.web.SessionCookie;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/rooms")
public class RoomController {

    private final RoomService roomService;
    private final SessionStore sessionStore;

    public RoomController(RoomService roomService, SessionStore sessionStore) {
        this.roomService = roomService;
        this.sessionStore = sessionStore;
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
        return ApiResponse.ok();
    }
}
