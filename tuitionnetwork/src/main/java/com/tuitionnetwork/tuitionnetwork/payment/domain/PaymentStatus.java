package com.tuitionnetwork.tuitionnetwork.payment.domain;
public enum PaymentStatus {
    CREATED,   // Payment has been created but not yet processed
    PENDING, // Payment is pending and awaiting processing
    AUTHORIZED, // Payment has been authorized but not yet captured
    CAPTURED,   // Payment has been captured
    DECLINED,   //BANK SAID NO
    VOIDED,     //CANCELLED BEFORE MONEY WAS TOKEN
    FAILED,     //MONEY WAS NOT TAKEN
    REFUNDED,   // MONEY WAS TAKEN AND RETURNED TO THE CUSTOMER
    EXPIRED,    // PAYMENT WAS NOT COMPLETED IN TIME
    
}
