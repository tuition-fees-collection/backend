package com.tuitionnetwork.tuitionnetwork.billing.service;

public class Concurrentsettlementexception extends RuntimeException {
 
    public Concurrentsettlementexception(String message) {
        super(message);
    }
 
    public Concurrentsettlementexception(String message, Throwable cause) {
        super(message, cause);
    }
}