package com.tuitionnetwork.tuitionnetwork.reporting.service;

import com.tuitionnetwork.tuitionnetwork.billing.domain.FeeType;
import com.tuitionnetwork.tuitionnetwork.payment.domain.PaymentMethod;
import com.tuitionnetwork.tuitionnetwork.reporting.domain.DailyCollectionRecord;
import com.tuitionnetwork.tuitionnetwork.reporting.domain.DailyCollectionReport;
import com.tuitionnetwork.tuitionnetwork.reporting.infra.DailyCollectionReportRepository;

import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Service 

public class DailyCollectionReportService {
    private final DailyCollectionReportRepository reportRepository;
    public DailyCollectionReportService(
        DailyCollectionReportRepository reportRepository
    ){
        this.reportRepository=reportRepository;
    }

    public DailyCollectionReport getDailyReport(
         UUID institutionId,
        LocalDate date
    ){
        ZoneId zone = ZoneId.of("Africa/Cairo");
        Instant start=date.atStartOfDay(zone).toInstant();
        Instant end=date.plusDays(1).atStartOfDay(zone).toInstant();
        List<DailyCollectionRecord> records=reportRepository.findDailyCollections(institutionId,start,end);
        BigDecimal totalCollected=records.stream().map(DailyCollectionRecord::amountApplied).reduce(BigDecimal.ZERO, BigDecimal::add);
        long paymentCount=records.stream().map(DailyCollectionRecord::paymentId).distinct().count();
        Map<FeeType,BigDecimal> amountByFeeType=records.stream().collect(Collectors.groupingBy(
            DailyCollectionRecord::feeType,
            Collectors.reducing(BigDecimal.ZERO,DailyCollectionRecord::amountApplied,BigDecimal::add)
        ));

        Map<PaymentMethod,BigDecimal> amountByPaymentmethod=records.stream().collect(Collectors.groupingBy(
             DailyCollectionRecord::paymentMethod,
            Collectors.reducing(BigDecimal.ZERO,DailyCollectionRecord::amountApplied,BigDecimal::add)
        ));

        return new DailyCollectionReport(
            date,
            totalCollected,
            paymentCount,
            amountByFeeType,
            amountByPaymentmethod,
            records
        );

    }
    
}
