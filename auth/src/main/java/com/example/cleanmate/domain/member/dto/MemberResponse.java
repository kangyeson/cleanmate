package com.example.cleanmate.domain.member.dto;

public record MemberResponse(
        Long id,
        String name,
        String role,
        int turnOrder,
        boolean active,
        boolean mustChangePw
) {}
