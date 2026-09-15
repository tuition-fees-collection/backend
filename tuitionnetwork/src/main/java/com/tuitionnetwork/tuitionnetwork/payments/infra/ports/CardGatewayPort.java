package com.tuitionnetwork.tuitionnetwork.payments.infra.ports;

import com.tuitionnetwork.tuitionnetwork.payments.infra.ports.bankmock.CardPaymentRequest;
import com.tuitionnetwork.tuitionnetwork.payments.infra.ports.bankmock.CardPaymentResponse;

import java.math.BigDecimal;

public interface CardGatewayPort {

    CardPaymentResponse charge(CardPaymentRequest request, String idempotencyKey);

    CardPaymentResponse capture(String gatewayPaymentId, BigDecimal amount);

    CardPaymentResponse confirm3ds(String gatewayPaymentId, String otp);

    CardPaymentResponse getStatus(String gatewayPaymentId);
}