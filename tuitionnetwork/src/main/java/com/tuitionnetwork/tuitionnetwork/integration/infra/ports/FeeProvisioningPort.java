package com.tuitionnetwork.tuitionnetwork.integration.infra.ports;

import java.math.BigDecimal;
import java.util.Objects;
import java.util.UUID;


public interface FeeProvisioningPort {

    default boolean isAvailable() {
        return true;
    }

    UUID provisionFee(Command command);

    record Command(
            String studentNationalId,
            UUID institutionId,
            String feeType,
            String collectionPeriod,
            String currency,
            BigDecimal amount,
            UUID sourceBatchId) {

        public Command {
            Objects.requireNonNull(studentNationalId, "studentNationalId");
            Objects.requireNonNull(institutionId, "institutionId");
            Objects.requireNonNull(feeType, "feeType");
            Objects.requireNonNull(collectionPeriod, "collectionPeriod");
            Objects.requireNonNull(currency, "currency");
            Objects.requireNonNull(amount, "amount");
            Objects.requireNonNull(sourceBatchId, "sourceBatchId");
        }
    }
}
