package com.example.cleanmate.domain.client;

import com.example.cleanmate.global.exception.CleanmateException;

/** auth 서버 명단 조회 실패 (503) */
public class RosterUnavailableException extends CleanmateException {

    public RosterUnavailableException() {
        super("잠시 후 다시 시도하세요.", 503);
    }
}
