package com.tuitionnetwork.tuitionnetwork.payments.infra.ports.bankmock;

import java.util.Map;

public record BankMockError(String code, String message, Map<String, Object> details) {
}
