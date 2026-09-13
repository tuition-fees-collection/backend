package com.tuitionnetwork.tuitionnetwork.identity.service;

public class GuardianNotFoundException
        extends RuntimeException {

    public GuardianNotFoundException() {
        super("Guardian not found");
    }
}
