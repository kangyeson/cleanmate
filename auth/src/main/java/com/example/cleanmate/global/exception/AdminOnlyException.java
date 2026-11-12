package com.example.cleanmate.global.exception;

public class AdminOnlyException extends CleanmateException {
    public AdminOnlyException() {
        super("관리자만 사용할 수 있습니다.", 403);
    }
}
