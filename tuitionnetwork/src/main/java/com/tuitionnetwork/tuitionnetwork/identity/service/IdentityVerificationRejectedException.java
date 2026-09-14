package com.tuitionnetwork.tuitionnetwork.identity.service;

public class IdentityVerificationRejectedException extends RuntimeException {

    public IdentityVerificationRejectedException() {
        super("Guardian identity could not be verified");
    }
}
