package com.example.cleanmate.global.apiPayload;

public record ApiResponse<T>(boolean success, T result, String message) {

    public static <T> ApiResponse<T> ok(T result) {
        return new ApiResponse<>(true, result, null);
    }

    public static ApiResponse<Void> ok() {
        return new ApiResponse<>(true, null, null);
    }

    public static ApiResponse<Void> error(String message) {
        return new ApiResponse<>(false, null, message);
    }
}
