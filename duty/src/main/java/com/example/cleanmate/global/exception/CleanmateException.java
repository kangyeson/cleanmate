package com.example.cleanmate.global.exception;

public class CleanmateException extends RuntimeException {

    private final int status;

    public CleanmateException(String message, int status) {
        super(message);
        this.status = status;
    }

    public int getStatus() {
        return status;
    }
}
