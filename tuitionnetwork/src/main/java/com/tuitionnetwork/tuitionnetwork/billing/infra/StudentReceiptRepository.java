package com.tuitionnetwork.tuitionnetwork.billing.infra;

import com.tuitionnetwork.tuitionnetwork.billing.api.dto.StudentReceiptLine;
import com.tuitionnetwork.tuitionnetwork.payment.domain.PaymentAllocation;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

public interface StudentReceiptRepository extends JpaRepository<PaymentAllocation, UUID> {

    @Query("""
            SELECT new com.tuitionnetwork.tuitionnetwork.billing.api.dto.StudentReceiptLine(
                p.id,
                r.referenceNumber,
                r.generatedAt,
                p.amount,
                p.currency,
                p.method,
                f.id,
                f.feeType,
                f.collectionPeriod,
                a.amountApplied
            )
            FROM PaymentAllocation a, Payment p, Fee f, Receipt r
            WHERE a.paymentId = p.id
            AND a.feeId = f.id
            AND r.paymentId = p.id
            AND f.studentId = :studentId
            AND f.institutionId = :institutionId
            AND p.status =
            com.tuitionnetwork.tuitionnetwork.payment.domain.PaymentStatus.SUCCESSFUL
            ORDER BY r.generatedAt DESC
            """)
    List<StudentReceiptLine> findByStudentAndInstitution(
            @Param("studentId") UUID studentId,
            @Param("institutionId") UUID institutionId
    );
}
