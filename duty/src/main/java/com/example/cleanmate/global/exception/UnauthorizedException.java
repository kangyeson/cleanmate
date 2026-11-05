package com.example.cleanmate.global.exception;

public class UnauthorizedException extends CleanmateException {

    public UnauthorizedException() {
        super("로그인이 필요합니다.", 401);
    }
}
