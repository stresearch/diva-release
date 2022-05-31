package com.visym.collector.exception;

public class InsufficientStorageException extends RuntimeException {

    public InsufficientStorageException() {
    }

    public InsufficientStorageException(String message) {
        super(message);
    }

    public InsufficientStorageException(String message, Throwable cause) {
        super(message, cause);
    }

    public InsufficientStorageException(Throwable cause) {
        super(cause);
    }
}
