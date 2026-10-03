package com.tuitionnetwork.tuitionnetwork.chatbot.service;

import java.math.BigDecimal;

public record OutstandingIncomeFigures(
        long outstandingCount,
        BigDecimal outstandingTotal,
        BigDecimal totalIncome
) {
}
