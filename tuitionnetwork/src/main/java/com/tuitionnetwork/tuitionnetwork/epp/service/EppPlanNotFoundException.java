package com.tuitionnetwork.tuitionnetwork.epp.service;

import java.util.UUID;

public class EppPlanNotFoundException extends RuntimeException {

    public EppPlanNotFoundException(UUID planId) {
        super("No such EPP plan: " + planId);
    }
}
