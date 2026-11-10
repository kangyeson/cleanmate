package com.example.cleanmate.domain.client;

import com.example.cleanmate.global.exception.CleanmateException;

/** auth 서버에서 진행 중인 방이 없다고 응답한 경우 (404) */
public class NoActiveRoomException extends CleanmateException {

    public NoActiveRoomException() {
        super("진행 중인 방이 없습니다.", 404);
    }
}
