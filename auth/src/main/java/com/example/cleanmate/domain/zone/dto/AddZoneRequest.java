package com.example.cleanmate.domain.zone.dto;

import jakarta.validation.constraints.NotBlank;

public record AddZoneRequest(
        @NotBlank(message = "구역 이름을 입력하세요.") String name
) {}
