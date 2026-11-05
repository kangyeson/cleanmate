package com.example.cleanmate.domain.client;

import com.example.cleanmate.global.exception.CleanmateException;

/** auth 서버 명단 조회 실패 (503) */
public class RosterUnavailableException extends CleanmateException {

    public RosterUnavailableException() {
        super("명단 조회 실패: auth 서버에 연결할 수 없습니다.", 503);
    }
}
