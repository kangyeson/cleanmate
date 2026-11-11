package com.example.cleanmate.domain.stats.controller;

import com.example.cleanmate.domain.stats.dto.StatsResponse;
import com.example.cleanmate.domain.stats.service.StatsService;
import com.example.cleanmate.global.apiPayload.ApiResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/stats")
public class StatsController {

    private final StatsService statsService;

    public StatsController(StatsService statsService) {
        this.statsService = statsService;
    }

    @GetMapping
    public ApiResponse<StatsResponse> getStats() {
        return ApiResponse.ok(statsService.getStats());
    }
}
