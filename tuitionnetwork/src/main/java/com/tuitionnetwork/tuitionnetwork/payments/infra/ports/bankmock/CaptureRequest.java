package com.tuitionnetwork.tuitionnetwork.payments.infra.ports.bankmock;

import java.math.BigDecimal;


public record CaptureRequest(BigDecimal amount) {
}
