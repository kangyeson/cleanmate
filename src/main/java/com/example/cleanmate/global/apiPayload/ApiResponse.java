package com.example.cleanmate.global.apiPayload;

public record ApiResponse<T>(boolean success, T result) {

    public static <T> ApiResponse<T> ok(T result) {
        return new ApiResponse<>(true, result);
    }

    public static ApiResponse<Void> ok() {
        return new ApiResponse<>(true, null);
    }
}
