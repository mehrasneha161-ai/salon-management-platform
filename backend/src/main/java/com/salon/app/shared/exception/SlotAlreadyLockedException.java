package com.salon.app.shared.exception;

public class SlotAlreadyLockedException extends RuntimeException {
    public SlotAlreadyLockedException(String message) {
        super(message);
    }
}
