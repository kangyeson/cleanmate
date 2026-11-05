package com.example.cleanmate.domain.duty.exception;

import com.example.cleanmate.global.exception.CleanmateException;

public class NotRecordOwnerException extends CleanmateException {
    public NotRecordOwnerException() {
        super("본인이 기록한 항목만 취소할 수 있습니다.", 403);
    }
}
