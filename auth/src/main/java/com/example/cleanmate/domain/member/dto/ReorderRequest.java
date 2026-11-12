package com.example.cleanmate.domain.member.dto;

import jakarta.validation.constraints.NotEmpty;

import java.util.List;

public record ReorderRequest(
        @NotEmpty List<Long> ids
) {}
