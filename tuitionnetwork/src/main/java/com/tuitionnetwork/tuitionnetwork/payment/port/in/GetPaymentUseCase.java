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

    /** Payment history, newest first - BO-US-12. */
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

    /** Which fee this payment cleared, and by how much. */
    record AllocationView(UUID feeId, String feeType, String collectionPeriod, BigDecimal amountApplied) {
    }

    /** The list row. Enough to render history without loading allocations. */
    record PaymentSummary(
            UUID paymentId,
            PaymentStatus status,
            BigDecimal amount,
            String currency,
            PaymentMethod method,
            Instant createdAt,
            boolean hasReceipt
    ) {
    }
}