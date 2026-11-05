package com.example.cleanmate.domain.auth.exception;

import com.example.cleanmate.global.exception.CleanmateException;

public class InvalidCredentialsException extends CleanmateException {

    public InvalidCredentialsException() {
        super("이름 또는 비밀번호가 맞지 않습니다.", 401);
    }
}
