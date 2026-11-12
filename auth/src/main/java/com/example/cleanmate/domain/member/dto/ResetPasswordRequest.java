package com.example.cleanmate.domain.member.dto;

import jakarta.validation.constraints.NotBlank;

public record ResetPasswordRequest(
        @NotBlank(message = "비밀번호를 입력하세요.") String password
) {}
