package com.example.cleanmate.domain.member.dto;

import jakarta.validation.constraints.NotBlank;

public record RenameMemberRequest(
        @NotBlank(message = "이름을 입력하세요.") String name
) {}
