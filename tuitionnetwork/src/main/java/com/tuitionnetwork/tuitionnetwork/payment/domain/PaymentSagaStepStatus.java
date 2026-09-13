package com.tuitionnetwork.tuitionnetwork.payment.domain;

public enum PaymentSagaStepStatus {
     PENDING, //the step is created and waiting to start
      IN_PROGRESS, //The system is actively working on the step right now.
      COMPLETED, //The step finished successfully.
      FAILED, //The step stopped because of an error.
       SKIPPED; //the step was intentionally bypassed because it wasn't needed
       //returns true if the step is in any end-state (COMPLETED, FAILED, or SKIPPED)
        public boolean isFinished() {
        return this == COMPLETED || this == FAILED || this == SKIPPED;
    }
}
