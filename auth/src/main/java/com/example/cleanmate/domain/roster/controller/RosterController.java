package com.example.cleanmate.domain.roster.controller;

import com.example.cleanmate.domain.room.service.RoomService;
import com.example.cleanmate.domain.roster.dto.RosterResponse;
import com.example.cleanmate.global.apiPayload.ApiResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * duty 서버 전용 내부 엔드포인트.
 * 진행 중인 방·룸메이트·구역 정보를 한 번에 반환.
 */
@RestController
public class RosterController {

    private final RoomService roomService;

    public RosterController(RoomService roomService) {
        this.roomService = roomService;
    }

    @GetMapping("/internal/roster")
    public ApiResponse<RosterResponse> getRoster() {
        return ApiResponse.ok(roomService.getRoster());
    }
}
