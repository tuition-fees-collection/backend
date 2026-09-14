package com.tuitionnetwork.tuitionnetwork.identity.infra.ports;

import java.util.Map;


public class MoiVerificationException extends RuntimeException {

    private final String code;
    private final Map<String, Object> details;

    public MoiVerificationException(String code, String message, Map<String, Object> details, Throwable cause) {
        super(message, cause);
        this.code = code;
        this.details = details == null ? Map.of() : Map.copyOf(details);
    }

    public String code() {
        return code;
    }

    public Map<String, Object> details() {
        return details;
    }
}
