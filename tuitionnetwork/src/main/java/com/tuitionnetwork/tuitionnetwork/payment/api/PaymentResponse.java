package com.tuitionnetwork.tuitionnetwork.payment.api;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import com.tuitionnetwork.tuitionnetwork.payment.domain.PaymentMethod;
import com.tuitionnetwork.tuitionnetwork.payment.domain.PaymentStatus;
import com.tuitionnetwork.tuitionnetwork.payment.domain.PaymentType;
import com.tuitionnetwork.tuitionnetwork.payment.port.in.CreatePaymentUseCase.PaymentOrderView;
import com.tuitionnetwork.tuitionnetwork.payment.port.in.GetPaymentUseCase.PaymentView;
import com.tuitionnetwork.tuitionnetwork.payment.port.in.GetPaymentUseCase.PaymentSummary;
public record PaymentResponse(
        UUID paymentId,
        PaymentStatus status,
        BigDecimal amount,
        String currency,
        PaymentMethod method,
        PaymentType paymentType,
        String maskedCardNumber,
        String cardScheme,
        Instant createdAt,
        Instant expiresAt,
        Instant settledAt,
        String receiptReference,
        List<AllocationResponse> allocations
) {

    public record AllocationResponse(
            UUID feeId,
            String feeType,
            String collectionPeriod,
            BigDecimal amountApplied) {
    }
    public static PaymentResponse from(PaymentOrderView view) {
        return new PaymentResponse(
                view.paymentId(),
                view.status(),
                view.amount(),
                view.currency(),
                view.method(),
                view.paymentType(),
                null,
                null,
                view.createdAt(),
                view.expiresAt(),
                null,
                null,
                List.of());
    }

    public static PaymentResponse from(PaymentView view) {
        return new PaymentResponse(
                view.paymentId(),
                view.status(),
                view.amount(),
                view.currency(),
                view.method(),
                view.paymentType(),
                view.maskedCardNumber(),
                view.cardScheme(),
                view.createdAt(),
                null,
                view.settledAt(),
                view.receiptReference(),
                view.allocations().stream()
                        .map(a -> new AllocationResponse(
                                a.feeId(), a.feeType(), a.collectionPeriod(), a.amountApplied()))
                        .toList());
    }
    public static PaymentResponse from(PaymentSummary summary) {
        return new PaymentResponse(
                summary.paymentId(),
                summary.status(),
                summary.amount(),
                summary.currency(),
                summary.method(),
                summary.paymentType(),
                summary.maskedCardNumber(),
                summary.cardScheme(),
                summary.createdAt(),
                null,
                summary.settledAt(),
                summary.receiptReference(),
                List.of());
    }
}