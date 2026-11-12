package com.example.cleanmate.domain.zone.dto;

public record ZoneResponse(
        Long id,
        String name,
        int zoneOrder,
        boolean active
) {}
