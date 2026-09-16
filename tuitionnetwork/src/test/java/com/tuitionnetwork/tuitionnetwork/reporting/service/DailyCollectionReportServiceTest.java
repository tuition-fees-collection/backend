package com.tuitionnetwork.tuitionnetwork.reporting.service;

import com.tuitionnetwork.tuitionnetwork.billing.domain.FeeType;
import com.tuitionnetwork.tuitionnetwork.payment.domain.PaymentMethod;
import com.tuitionnetwork.tuitionnetwork.reporting.domain.DailyCollectionRecord;
import com.tuitionnetwork.tuitionnetwork.reporting.domain.DailyCollectionReport;
import com.tuitionnetwork.tuitionnetwork.reporting.infra.DailyCollectionReportRepository;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

class DailyCollectionReportServiceTest {

    private DailyCollectionReportRepository reportRepository;
    private DailyCollectionReportService reportService;

    @BeforeEach
    void setUp() {
        reportRepository = mock(DailyCollectionReportRepository.class);
        reportService = new DailyCollectionReportService(reportRepository);
    }

    @Test
    void getDailyReport_CalculatesSummaryCorrectly() {

        UUID institutionId = UUID.randomUUID();

        UUID payment1 = UUID.randomUUID();
        UUID payment2 = UUID.randomUUID();

        UUID student1 = UUID.randomUUID();
        UUID student2 = UUID.randomUUID();

        LocalDate date = LocalDate.of(2026, 9, 16);

        List<DailyCollectionRecord> records = List.of(

                new DailyCollectionRecord(
                        payment1,
                        student1,
                        FeeType.TUITION,
                        new BigDecimal("8000.00"),
                        PaymentMethod.CREDIT_CARD,
                        Instant.parse("2026-09-16T08:00:00Z"),
                         "RRN-001"
                ),

                new DailyCollectionRecord(
                        payment1,
                        student1,
                        FeeType.TRANSPORTATION,
                        new BigDecimal("2000.00"),
                        PaymentMethod.CREDIT_CARD,
                        Instant.parse("2026-09-16T08:00:00Z"),
                         "RRN-001"
                ),

                new DailyCollectionRecord(
                        payment2,
                        student2,
                        FeeType.TUITION,
                        new BigDecimal("5000.00"),
                        PaymentMethod.CASH,
                        Instant.parse("2026-09-16T10:00:00Z"),
                         "RRN-002"
                )
        );

        when(reportRepository.findDailyCollections(
                eq(institutionId),
                any(Instant.class),
                any(Instant.class)
        )).thenReturn(records);

        DailyCollectionReport result =
                reportService.getDailyReport(
                        institutionId,
                        date
                );

        assertEquals(date, result.date());

        assertEquals(
                0,
                new BigDecimal("15000.00")
                        .compareTo(result.totalCollected())
        );

        // payment1 appears twice but counts as one payment
        assertEquals(2, result.paymentCount());

        assertEquals(
                0,
                new BigDecimal("13000.00")
                        .compareTo(
                                result.amountByFeeType()
                                        .get(FeeType.TUITION)
                        )
        );

        assertEquals(
                0,
                new BigDecimal("2000.00")
                        .compareTo(
                                result.amountByFeeType()
                                        .get(FeeType.TRANSPORTATION)
                        )
        );

        assertEquals(
                0,
                new BigDecimal("10000.00")
                        .compareTo(
                                result.amountByPaymentMethod()
                                        .get(PaymentMethod.CREDIT_CARD)
                        )
        );

        assertEquals(
                0,
                new BigDecimal("5000.00")
                        .compareTo(
                                result.amountByPaymentMethod()
                                        .get(PaymentMethod.CASH)
                        )
        );

        assertEquals(3, result.items().size());

        verify(reportRepository).findDailyCollections(
                eq(institutionId),
                any(Instant.class),
                any(Instant.class)
        );
    }

    @Test
void getDailyReport_NoPayments_ReturnsEmptyReport() {

    UUID institutionId = UUID.randomUUID();
    LocalDate date = LocalDate.of(2026, 9, 16);

    when(reportRepository.findDailyCollections(
            eq(institutionId),
            any(Instant.class),
            any(Instant.class)
    )).thenReturn(List.of());

    DailyCollectionReport result =
            reportService.getDailyReport(
                    institutionId,
                    date
            );

    assertEquals(date, result.date());

    assertEquals(
            0,
            BigDecimal.ZERO.compareTo(result.totalCollected())
    );

    assertEquals(0, result.paymentCount());

    assertTrue(result.amountByFeeType().isEmpty());

    assertTrue(result.amountByPaymentMethod().isEmpty());

    assertTrue(result.items().isEmpty());

    verify(reportRepository).findDailyCollections(
            eq(institutionId),
            any(Instant.class),
            any(Instant.class)
    );
}

@Test
void getDailyReport_UsesCorrectCairoDayBoundaries() {

    UUID institutionId = UUID.randomUUID();
    LocalDate date = LocalDate.of(2026, 9, 16);

    when(reportRepository.findDailyCollections(
            eq(institutionId),
            any(Instant.class),
            any(Instant.class)
    )).thenReturn(List.of());

    reportService.getDailyReport(institutionId, date);

    Instant expectedStart = date
            .atStartOfDay(ZoneId.of("Africa/Cairo"))
            .toInstant();

    Instant expectedEnd = date
            .plusDays(1)
            .atStartOfDay(ZoneId.of("Africa/Cairo"))
            .toInstant();

    verify(reportRepository).findDailyCollections(
            institutionId,
            expectedStart,
            expectedEnd
    );
}
}