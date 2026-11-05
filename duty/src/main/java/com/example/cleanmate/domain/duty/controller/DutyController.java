package com.example.cleanmate.domain.duty.controller;

import com.example.cleanmate.domain.duty.dto.WeekDutyResponse;
import com.example.cleanmate.domain.duty.service.DutyService;
import com.example.cleanmate.global.apiPayload.ApiResponse;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/duty")
public class DutyController {

    private final DutyService dutyService;

    public DutyController(DutyService dutyService) {
        this.dutyService = dutyService;
    }

    /** 이번 주 청소 당번 현황 (로그인 불필요) */
    @GetMapping("/week")
    public ApiResponse<WeekDutyResponse> getWeekDuties() {
        return ApiResponse.ok(dutyService.getWeekDuties());
    }

    /**
     * 본인 담당 구역 완료 처리.
     * TODO: X-Member-Id 헤더는 임시. 2단계에서 세션(CMSESSION)으로 교체.
     */
    @PostMapping("/{zoneId}/done")
    public ApiResponse<Void> markDone(
            @PathVariable Long zoneId,
            @RequestHeader("X-Member-Id") Long memberId) {
        dutyService.markDone(zoneId, memberId);
        return ApiResponse.ok();
    }

    /**
     * 대신 청소 처리.
     * TODO: X-Member-Id 헤더는 임시. 2단계에서 세션(CMSESSION)으로 교체.
     */
    @PostMapping("/{zoneId}/substitute")
    public ApiResponse<Void> markSubstitute(
            @PathVariable Long zoneId,
            @RequestHeader("X-Member-Id") Long memberId) {
        dutyService.markSubstitute(zoneId, memberId);
        return ApiResponse.ok();
    }

    /**
     * 완료 기록 취소 (본인 기록만).
     * TODO: X-Member-Id 헤더는 임시. 2단계에서 세션(CMSESSION)으로 교체.
     */
    @DeleteMapping("/{zoneId}/record")
    public ApiResponse<Void> cancelRecord(
            @PathVariable Long zoneId,
            @RequestHeader("X-Member-Id") Long memberId) {
        dutyService.cancelRecord(zoneId, memberId);
        return ApiResponse.ok();
    }
}
