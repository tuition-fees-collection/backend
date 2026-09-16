package com.tuitionnetwork.tuitionnetwork.reporting.service;

import com.tuitionnetwork.tuitionnetwork.billing.domain.FeeType;
import com.tuitionnetwork.tuitionnetwork.payment.domain.PaymentMethod;
import com.tuitionnetwork.tuitionnetwork.reporting.domain.DailyCollectionRecord;
import com.tuitionnetwork.tuitionnetwork.reporting.domain.DailyCollectionReport;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class DailyCollectionCsvServiceTest {

    private final DailyCollectionCsvService csvService =
            new DailyCollectionCsvService();

    @Test
    void generateCsv_ReturnsHeaderAndCollectionRows() {

        UUID studentId = UUID.randomUUID();
        UUID paymentId = UUID.randomUUID();

        DailyCollectionRecord record =
                new DailyCollectionRecord(
                        paymentId,
                        studentId,
                        FeeType.TUITION,
                        new BigDecimal("4000.00"),
                        PaymentMethod.CREDIT_CARD,
                        Instant.parse("2026-09-16T10:30:00Z"),
                        "3355"
                );

        DailyCollectionReport report =
                new DailyCollectionReport(
                        LocalDate.of(2026, 9, 16),
                        new BigDecimal("4000.00"),
                        1,
                        Map.of(
                                FeeType.TUITION,
                                new BigDecimal("4000.00")
                        ),
                        Map.of(
                                PaymentMethod.CREDIT_CARD,
                                new BigDecimal("4000.00")
                        ),
                        List.of(record)
                );

        byte[] result = csvService.generateCsv(report);

        String csv = new String(
                result,
                StandardCharsets.UTF_8
        );

        assertTrue(csv.contains(
                "Student ID,Fee Type,Amount,Payment Method,Settled At,Payment Reference"
        ));

        assertTrue(csv.contains(studentId.toString()));
        assertTrue(csv.contains("TUITION"));
        assertTrue(csv.contains("4000.00"));
        assertTrue(csv.contains("CREDIT_CARD"));
        assertTrue(csv.contains("2026-09-16T10:30:00Z"));
        assertTrue(csv.contains("3355"));
    }

    @Test
void generateCsv_EmptyReport_StillReturnsCsvWithHeader() {

    DailyCollectionReport report =
            new DailyCollectionReport(
                    LocalDate.of(2026, 9, 16),
                    BigDecimal.ZERO,
                    0,
                    Map.of(),
                    Map.of(),
                    List.of()
            );

    byte[] result = csvService.generateCsv(report);

    String csv = new String(
            result,
            StandardCharsets.UTF_8
    );

    assertFalse(csv.isBlank());

    assertTrue(csv.contains(
            "Student ID,Fee Type,Amount,Payment Method,Settled At,Payment Reference"
    ));

    // Header only: there should be no collection rows
    assertEquals(
            "Student ID,Fee Type,Amount,Payment Method,Settled At,Payment Reference\n",
            csv
    );
}
}