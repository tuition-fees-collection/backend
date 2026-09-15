package com.tuitionnetwork.tuitionnetwork.billing.service;

public class FeeNotFoundException extends RuntimeException {

    public FeeNotFoundException(String message) {
        super(message);
    }
}