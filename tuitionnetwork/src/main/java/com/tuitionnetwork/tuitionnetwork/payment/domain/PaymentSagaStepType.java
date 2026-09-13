package com.tuitionnetwork.tuitionnetwork.payment.domain;

public enum PaymentSagaStepType {
VERIFY_ACCESS, //Checks if the user has permission to make this payment.
VERIFY_PAYABLE, //Checks if the tuition fee is valid, unpaid, and ready to be paid.
CHARGE, //Requests and transfers the money from the customer's card or bank account.
CONFIRM_3DS, //Verifies that the customer passed their bank's OTP security authentication.
SETTLE_FEES, //Applies the collected money to clear the unpaid tuition balance in  database.
ISSUE_RECEIPT,//Generates an official payment receipt .
PUBLISH_EVENT; //Sends notifications.
 public boolean isMoneyMoving() {
        return this == CHARGE || this == CONFIRM_3DS;
    }
}
