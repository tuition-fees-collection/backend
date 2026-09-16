package com.tuitionnetwork.tuitionnetwork.reporting.service;

import com.tuitionnetwork.tuitionnetwork.billing.domain.FeeType;
import com.tuitionnetwork.tuitionnetwork.payment.domain.PaymentMethod;
import com.tuitionnetwork.tuitionnetwork.reporting.domain.DailyCollectionRecord;
import com.tuitionnetwork.tuitionnetwork.reporting.domain.DailyCollectionReport;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class DailyCollectionPdfServiceTest {

    private final DailyCollectionPdfService pdfService =
            new DailyCollectionPdfService();

    @Test
    void generatePdf_ReturnsValidPdf() {

        DailyCollectionRecord item =
                new DailyCollectionRecord(
                        UUID.randomUUID(),
                        UUID.randomUUID(),
                        FeeType.TUITION,
                        new BigDecimal("4000.00"),
                        PaymentMethod.CREDIT_CARD,
                        Instant.parse("2026-09-16T10:00:00Z"),
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
                        List.of(item)
                );

        byte[] pdf = pdfService.generatePdf(report);

        assertNotNull(pdf);
        assertTrue(pdf.length > 0);

        assertEquals('%', (char) pdf[0]);
        assertEquals('P', (char) pdf[1]);
        assertEquals('D', (char) pdf[2]);
        assertEquals('F', (char) pdf[3]);
        assertEquals('-', (char) pdf[4]);
    }

    @Test
void generatePdf_EmptyReport_ReturnsValidPdf() {

    DailyCollectionReport report =
            new DailyCollectionReport(
                    LocalDate.of(2026, 9, 16),
                    BigDecimal.ZERO,
                    0,
                    Map.of(),
                    Map.of(),
                    List.of()
            );

    byte[] pdf = pdfService.generatePdf(report);

    assertNotNull(pdf);
    assertTrue(pdf.length > 0);

    assertEquals('%', (char) pdf[0]);
    assertEquals('P', (char) pdf[1]);
    assertEquals('D', (char) pdf[2]);
    assertEquals('F', (char) pdf[3]);
    assertEquals('-', (char) pdf[4]);
}
}