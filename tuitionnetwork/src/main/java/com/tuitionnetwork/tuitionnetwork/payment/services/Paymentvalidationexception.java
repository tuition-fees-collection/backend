package com.tuitionnetwork.tuitionnetwork.payment.services;
public class Paymentvalidationexception extends RuntimeException {
    private final String code;
    public Paymentvalidationexception(String code, String message) {
        super(message);
        this.code = code;
    }
    public String getCode() {
        return code;
    }
}
