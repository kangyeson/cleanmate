package com.example.cleanmate.domain.auth.dto;

public record MeResponse(Long memberId, String name, String role, boolean mustChangePw) {
}
