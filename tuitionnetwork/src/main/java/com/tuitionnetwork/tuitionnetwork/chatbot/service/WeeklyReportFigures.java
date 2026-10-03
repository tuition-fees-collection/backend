package com.tuitionnetwork.tuitionnetwork.chatbot.service;

import java.math.BigDecimal;
import java.util.List;

public record WeeklyReportFigures(
        BigDecimal weekTotal,
        long transactionCount,
        List<DailyPoint> dailyTotals
) {
}
