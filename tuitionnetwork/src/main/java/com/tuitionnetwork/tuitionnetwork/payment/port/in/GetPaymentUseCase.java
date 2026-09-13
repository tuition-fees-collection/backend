package com.tuitionnetwork.tuitionnetwork.payment.port.in;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import com.tuitionnetwork.tuitionnetwork.payment.domain.PaymentMethod;
import com.tuitionnetwork.tuitionnetwork.payment.domain.PaymentStatus;
import com.tuitionnetwork.tuitionnetwork.payment.domain.PaymentType;
public interface GetPaymentUseCase {

        /**
     * @throws com.tuitionnetwork.tuitionnetwork.payment.Services.PaymentAccessDeniedException
     *         if the payment belongs to a different guardian
     */
    PaymentView getById(UUID paymentId, UUID requestingGuardianId);
    List<PaymentSummary> history(UUID guardianId);
    record PaymentView(
            UUID paymentId,
            PaymentStatus status,
            BigDecimal amount,
            String currency,
            PaymentMethod method,
            PaymentType paymentType,
            String maskedCardNumber,
            String cardScheme,
            Instant createdAt,
            Instant settledAt,
            String receiptReference,
            List<AllocationView> allocations
    ) {
        public PaymentView {
            allocations = allocations == null ? List.of() : List.copyOf(allocations);
        }
    }
    record AllocationView(UUID feeId, String feeType, String collectionPeriod, BigDecimal amountApplied) {
    }
    record PaymentSummary(
            UUID paymentId,
            PaymentStatus status,
            BigDecimal amount,
            String currency,
            PaymentMethod method,
            PaymentType paymentType,
            String maskedCardNumber,
            String cardScheme,
            Instant createdAt,
            Instant settledAt,
            String receiptReference
    ) {
        public boolean hasReceipt() {
            return receiptReference != null;
        }
    }
}