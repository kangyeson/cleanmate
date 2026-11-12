package com.example.cleanmate.domain.room.controller;

import com.example.cleanmate.domain.duty.service.DutyService;
import com.example.cleanmate.global.apiPayload.ApiResponse;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class InternalRoomController {

    private final DutyService dutyService;

    public InternalRoomController(DutyService dutyService) {
        this.dutyService = dutyService;
    }

    @PostMapping("/internal/rooms/{roomId}/finalize")
    public ApiResponse<Void> finalizeRoom(@PathVariable Long roomId) {
        dutyService.finalizeRoom(roomId);
        return ApiResponse.ok();
    }

    @PostMapping("/internal/rooms/{roomId}/current-week")
    public ApiResponse<Void> ensureCurrentWeek(@PathVariable Long roomId) {
        dutyService.ensureCurrentWeek(roomId);
        return ApiResponse.ok();
    }
}
