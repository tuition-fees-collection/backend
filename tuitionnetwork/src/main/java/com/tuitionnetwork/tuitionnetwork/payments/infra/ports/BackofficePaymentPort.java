package com.tuitionnetwork.tuitionnetwork.payments.infra.ports;

import com.tuitionnetwork.tuitionnetwork.payments.infra.ports.bankmock.BackofficePaymentRequest;
import com.tuitionnetwork.tuitionnetwork.payments.infra.ports.bankmock.BackofficePaymentResponse;

import java.math.BigDecimal;
import java.util.List;

public interface BackofficePaymentPort {

    BackofficePaymentResponse charge(BackofficePaymentRequest request, String idempotencyKey);

    BackofficePaymentResponse capture(String paymentId, BigDecimal amount);

    BackofficePaymentResponse getStatus(String paymentId);

    List<BackofficePaymentResponse> list(String customerId, String sourceId, String status);
}
