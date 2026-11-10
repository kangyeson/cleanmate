package com.example.cleanmate.domain.room.exception;

import com.example.cleanmate.global.exception.CleanmateException;

/**
 * 이미 진행 중인 방이 있을 때 발생하는 예외 (409 Conflict).
 */
public class RoomAlreadyActiveException extends CleanmateException {

    public RoomAlreadyActiveException() {
        super("이미 진행 중인 방이 있습니다.", 409);
    }
}
