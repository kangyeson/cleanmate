package com.example.cleanmate.global.exception;

public class PasswordChangeRequiredException extends CleanmateException {

    public PasswordChangeRequiredException() {
        super("비밀번호를 먼저 변경하세요.", 403);
    }
}
