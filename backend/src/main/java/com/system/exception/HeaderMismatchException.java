package com.system.exception;

public class HeaderMismatchException extends RuntimeException {
    public HeaderMismatchException(String message) {
        super(message);
    }
}
