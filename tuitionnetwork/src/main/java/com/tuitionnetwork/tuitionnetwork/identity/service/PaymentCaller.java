package com.tuitionnetwork.tuitionnetwork.identity.service;

import com.tuitionnetwork.tuitionnetwork.identity.domain.RoleCode;

import java.util.UUID;

public record PaymentCaller(UUID guardianId, UUID processedBy, RoleCode role) {

    public boolean isPayer() {
        return role == RoleCode.PAYER;
    }

    public boolean isBankOfficer() {
        return role == RoleCode.BANK_OFFICER;
    }
}
