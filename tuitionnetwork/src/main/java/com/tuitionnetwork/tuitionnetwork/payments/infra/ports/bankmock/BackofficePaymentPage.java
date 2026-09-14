package com.tuitionnetwork.tuitionnetwork.payments.infra.ports.bankmock;

import java.util.List;

public record BackofficePaymentPage(
        int total,
        int limit,
        int offset,
        List<BackofficePaymentResponse> items) {
}
