package com.tuitionnetwork.tuitionnetwork.chatbot.infra;

import com.tuitionnetwork.tuitionnetwork.chatbot.service.ChatbotFiguresSource;
import com.tuitionnetwork.tuitionnetwork.chatbot.service.DailyCollectionFigures;
import com.tuitionnetwork.tuitionnetwork.chatbot.service.DailyPoint;
import com.tuitionnetwork.tuitionnetwork.chatbot.service.OutstandingIncomeFigures;
import com.tuitionnetwork.tuitionnetwork.chatbot.service.WeeklyReportFigures;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Repository
public class ChatbotAggregateRepository implements ChatbotFiguresSource {

    static final ZoneId CAIRO = ZoneId.of("Africa/Cairo");

    private static final String DAILY_SQL = """
            SELECT COALESCE(SUM(a.amount_applied), 0) AS total_collected,
                   COUNT(DISTINCT p.id) AS transaction_count
            FROM payments.payment_allocation a
            JOIN payments.payment p ON p.id = a.payment_id
            JOIN billing.fee f ON f.id = a.fee_id
            WHERE f.institution_id = :institutionId
              AND p.status = 'SUCCESSFUL'
              AND p.settled_at >= :start
              AND p.settled_at < :end
            """;

    private static final String OUTSTANDING_SQL = """
            SELECT COUNT(DISTINCT f.student_id) AS outstanding_count,
                   COALESCE(SUM(f.amount - f.amount_paid), 0) AS outstanding_total
            FROM billing.fee f
            WHERE f.institution_id = :institutionId
              AND f.status IN ('OUTSTANDING', 'PARTIALLY_PAID')
              AND f.amount > f.amount_paid
            """;

    private static final String INCOME_SQL = """
            SELECT COALESCE(SUM(a.amount_applied), 0) AS total_income
            FROM payments.payment_allocation a
            JOIN payments.payment p ON p.id = a.payment_id
            JOIN billing.fee f ON f.id = a.fee_id
            WHERE f.institution_id = :institutionId
              AND p.status = 'SUCCESSFUL'
            """;

    private static final String WEEK_SQL = """
            SELECT (p.settled_at AT TIME ZONE 'Africa/Cairo')::date AS day,
                   COALESCE(SUM(a.amount_applied), 0) AS total,
                   COUNT(DISTINCT p.id) AS transaction_count
            FROM payments.payment_allocation a
            JOIN payments.payment p ON p.id = a.payment_id
            JOIN billing.fee f ON f.id = a.fee_id
            WHERE f.institution_id = :institutionId
              AND p.status = 'SUCCESSFUL'
              AND p.settled_at >= :start
              AND p.settled_at < :end
            GROUP BY (p.settled_at AT TIME ZONE 'Africa/Cairo')::date
            """;

    private final JdbcClient jdbcClient;

    public ChatbotAggregateRepository(JdbcClient jdbcClient) {
        this.jdbcClient = jdbcClient;
    }

    @Override
    public DailyCollectionFigures daily(UUID institutionId) {
        LocalDate today = LocalDate.now(CAIRO);
        Instant start = today.atStartOfDay(CAIRO).toInstant();
        Instant end = today.plusDays(1).atStartOfDay(CAIRO).toInstant();
        return jdbcClient.sql(DAILY_SQL)
                .param("institutionId", institutionId)
                .param("start", start)
                .param("end", end)
                .query((rs, row) -> new DailyCollectionFigures(
                        rs.getBigDecimal("total_collected"),
                        rs.getLong("transaction_count")))
                .single();
    }

    @Override
    public OutstandingIncomeFigures outstandingAndIncome(UUID institutionId) {
        OutstandingParts outstanding = jdbcClient.sql(OUTSTANDING_SQL)
                .param("institutionId", institutionId)
                .query((rs, row) -> new OutstandingParts(
                        rs.getLong("outstanding_count"),
                        rs.getBigDecimal("outstanding_total")))
                .single();
        BigDecimal income = jdbcClient.sql(INCOME_SQL)
                .param("institutionId", institutionId)
                .query((rs, row) -> rs.getBigDecimal("total_income"))
                .single();
        return new OutstandingIncomeFigures(
                outstanding.count(),
                outstanding.total(),
                income);
    }

    @Override
    public WeeklyReportFigures weekly(UUID institutionId) {
        LocalDate today = LocalDate.now(CAIRO);
        LocalDate startDay = today.minusDays(6);
        Instant start = startDay.atStartOfDay(CAIRO).toInstant();
        Instant end = today.plusDays(1).atStartOfDay(CAIRO).toInstant();
        Map<LocalDate, DayRow> byDay = new HashMap<>();
        jdbcClient.sql(WEEK_SQL)
                .param("institutionId", institutionId)
                .param("start", start)
                .param("end", end)
                .query((rs, row) -> {
                    LocalDate day = rs.getObject("day", LocalDate.class);
                    byDay.put(day, new DayRow(rs.getBigDecimal("total"), rs.getLong("transaction_count")));
                    return day;
                })
                .list();
        List<DailyPoint> days = new ArrayList<>();
        BigDecimal weekTotal = BigDecimal.ZERO;
        long transactions = 0;
        for (int i = 0; i < 7; i++) {
            LocalDate day = startDay.plusDays(i);
            DayRow row = byDay.get(day);
            BigDecimal total = row == null ? BigDecimal.ZERO : row.total();
            days.add(new DailyPoint(day, total));
            weekTotal = weekTotal.add(total);
            if (row != null) {
                transactions += row.transactionCount();
            }
        }
        return new WeeklyReportFigures(weekTotal, transactions, List.copyOf(days));
    }

    private record OutstandingParts(long count, BigDecimal total) {
    }

    private record DayRow(BigDecimal total, long transactionCount) {
    }
}
