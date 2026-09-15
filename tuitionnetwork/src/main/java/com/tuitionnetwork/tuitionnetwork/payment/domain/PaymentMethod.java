package com.tuitionnetwork.tuitionnetwork.payment.domain;
public enum PaymentMethod {
    CASH,
    CREDIT_CARD,
    DEBIT_CARD,
    ACCOUNT;
    public boolean requiresGateway() {
        return this != CASH;
    }
    public boolean isEppEligible() {
        return this == CREDIT_CARD;
    }
    public boolean usesHeldSource() {
        return this == ACCOUNT;
    }
}