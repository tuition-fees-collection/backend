package com.tuitionnetwork.tuitionnetwork.reporting.infra;

import com.tuitionnetwork.tuitionnetwork.payment.domain.PaymentAllocation;
import com.tuitionnetwork.tuitionnetwork.reporting.domain.DailyCollectionRecord;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public interface DailyCollectionReportRepository 

    extends JpaRepository<PaymentAllocation, UUID>{
        @Query("""
                SELECT new com.tuitionnetwork.tuitionnetwork.reporting.domain.DailyCollectionRecord(
                 p.id,
                 f.studentId,
                 f.feeType,
                 a.amountApplied,
                 p.method,
                 p.settledAt,
                 p.rrn
                )
                FROM PaymentAllocation a, Payment p, Fee f
                WHERE a.paymentId = p.id
                AND a.feeId = f.id
                AND f.institutionId = :institutionId
                AND p.status =
                com.tuitionnetwork.tuitionnetwork.payment.domain.PaymentStatus.SUCCESSFUL
                AND p.settledAt >= :start
                AND p.settledAt < :end
                ORDER BY p.settledAt ASC
             """)

             List<DailyCollectionRecord> findDailyCollections(
                @Param("institutionId") UUID institutionId,
                @Param ("start") Instant start,
                @Param ("end") Instant end
             );

    }
