package com.system.exception;

public class DuplicateTableNameException extends RuntimeException {
    public DuplicateTableNameException(String message) {
        super(message);
    }
}
