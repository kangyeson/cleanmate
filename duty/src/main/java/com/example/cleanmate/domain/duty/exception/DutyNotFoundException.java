package com.example.cleanmate.domain.duty.exception;

import com.example.cleanmate.global.exception.CleanmateException;

public class DutyNotFoundException extends CleanmateException {
    public DutyNotFoundException() {
        super("해당 구역의 이번 주 배정을 찾을 수 없습니다.", 404);
    }
}
