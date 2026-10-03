package com.tuitionnetwork.tuitionnetwork.chatbot.service;

import java.math.BigDecimal;
import java.time.LocalDate;

public record DailyPoint(LocalDate date, BigDecimal total) {
}
