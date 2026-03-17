package com.foo.kycmatch.exception;

public class ReportWriteException extends RuntimeException {

    public ReportWriteException(String message, Throwable cause) {
        super(message, cause);
    }
}
