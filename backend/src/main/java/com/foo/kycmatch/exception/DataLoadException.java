package com.foo.kycmatch.exception;

public class DataLoadException extends RuntimeException {

    public DataLoadException(String message, Throwable cause) {
        super(message, cause);
    }
}
