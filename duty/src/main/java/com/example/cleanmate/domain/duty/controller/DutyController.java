package com.example.cleanmate.domain.duty.controller;

import com.example.cleanmate.domain.duty.dto.WeekDutyResponse;
import com.example.cleanmate.domain.duty.service.DutyService;
import com.example.cleanmate.global.apiPayload.ApiResponse;
import com.example.cleanmate.global.interceptor.SessionInterceptor;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

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

    /** 내 담당 구역 (로그인 필요 — SessionInterceptor가 memberId를 request에 담음) */
    @GetMapping("/mine")
    public ApiResponse<WeekDutyResponse.DutyItem> getMyDuty(HttpServletRequest request) {
        Long memberId = (Long) request.getAttribute(SessionInterceptor.MEMBER_ATTR);
        return ApiResponse.ok(dutyService.getMyDuty(memberId));
    }

    /** 본인 담당 구역 완료 처리 */
    @PostMapping("/{zoneId}/done")
    public ApiResponse<Void> markDone(
            @PathVariable Long zoneId,
            HttpServletRequest request) {
        Long memberId = (Long) request.getAttribute(SessionInterceptor.MEMBER_ATTR);
        dutyService.markDone(zoneId, memberId);
        return ApiResponse.ok();
    }

    /** 대신 청소 처리 */
    @PostMapping("/{zoneId}/substitute")
    public ApiResponse<Void> markSubstitute(
            @PathVariable Long zoneId,
            HttpServletRequest request) {
        Long memberId = (Long) request.getAttribute(SessionInterceptor.MEMBER_ATTR);
        dutyService.markSubstitute(zoneId, memberId);
        return ApiResponse.ok();
    }

    /** 완료 기록 취소 (본인 기록만) */
    @DeleteMapping("/{zoneId}/record")
    public ApiResponse<Void> cancelRecord(
            @PathVariable Long zoneId,
            HttpServletRequest request) {
        Long memberId = (Long) request.getAttribute(SessionInterceptor.MEMBER_ATTR);
        dutyService.cancelRecord(zoneId, memberId);
        return ApiResponse.ok();
    }
}
