package com.example.cleanmate.domain.duty.controller;

import com.example.cleanmate.domain.duty.dto.WeekDutyResponse;
import com.example.cleanmate.domain.duty.service.DutyService;
import com.example.cleanmate.global.apiPayload.ApiResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/duty")
public class DutyController {

    private final DutyService dutyService;

    public DutyController(DutyService dutyService) {
        this.dutyService = dutyService;
    }

    /** 이번 주 청소 당번 현황 */
    @GetMapping("/week")
    public ApiResponse<WeekDutyResponse> getWeekDuties() {
        return ApiResponse.ok(dutyService.getWeekDuties());
    }
}
