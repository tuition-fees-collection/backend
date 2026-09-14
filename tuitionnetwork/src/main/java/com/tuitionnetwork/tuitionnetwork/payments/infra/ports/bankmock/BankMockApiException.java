package com.tuitionnetwork.tuitionnetwork.payments.infra.ports.bankmock;

import java.util.Map;


public class BankMockApiException extends RuntimeException {

    private final String code;
    private final Map<String, Object> details;

    public BankMockApiException(String code, String message, Map<String, Object> details) {
        super(message);
        this.code = code;
        this.details = details;
    }

    public String code() {
        return code;
    }

    public Map<String, Object> details() {
        return details;
    }
}
