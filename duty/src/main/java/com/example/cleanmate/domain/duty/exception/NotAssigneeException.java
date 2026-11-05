package com.example.cleanmate.domain.duty.exception;

import com.example.cleanmate.global.exception.CleanmateException;

public class NotAssigneeException extends CleanmateException {
    public NotAssigneeException() {
        super("본인 담당 구역이 아닙니다.", 403);
    }
}
