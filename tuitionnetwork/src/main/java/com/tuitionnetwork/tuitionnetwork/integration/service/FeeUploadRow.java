package com.tuitionnetwork.tuitionnetwork.integration.service;

import java.math.BigDecimal;


public record FeeUploadRow(
        int rowNumber,
        String nationalId,
        String feeType,
        BigDecimal amount,
        String currency,
        String collectionPeriod,
        String parentNationalId,
        String parentFullName) {
}
