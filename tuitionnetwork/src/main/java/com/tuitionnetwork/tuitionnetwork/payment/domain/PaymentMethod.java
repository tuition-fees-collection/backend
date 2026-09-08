package com.tuitionnetwork.tuitionnetwork.payment.domain;
public enum PaymentMethod {
       CASH,
        CREDIT_CARD,
        DEBIT_CARD;
        public boolean requiresGateway() {
        return this != CASH;
    }
 
  //a debit card never sees the instalment option. 
    public boolean isEppEligible() {
        return this == CREDIT_CARD;
    }
}
