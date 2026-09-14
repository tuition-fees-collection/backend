package com.tuitionnetwork.tuitionnetwork.integration.service;

public class UploadValidationException extends RuntimeException {

    private final String code;

    public UploadValidationException(String code, String message) {
        super(message);
        this.code = code;
    }

    public UploadValidationException(String code, String message, Throwable cause) {
        super(message, cause);
        this.code = code;
    }

    public String code() {
        return code;
    }
}
