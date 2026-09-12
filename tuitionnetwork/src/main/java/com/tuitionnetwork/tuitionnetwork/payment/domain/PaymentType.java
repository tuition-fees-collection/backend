package com.tuitionnetwork.tuitionnetwork.payment.domain;

public enum PaymentType {
    FULL, // Payment for the full amount of the tuition fee
    PARTIAL, // Payment for a portion of the tuition fee
    EPP;
      public boolean leavesRemainder() {
        return this == PARTIAL;
    }

}
