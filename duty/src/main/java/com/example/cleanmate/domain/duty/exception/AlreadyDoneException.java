package com.example.cleanmate.domain.duty.exception;

import com.example.cleanmate.global.exception.CleanmateException;

public class AlreadyDoneException extends CleanmateException {
    public AlreadyDoneException() {
        super("이미 완료되었거나 확정된 구역입니다.", 409);
    }
}
