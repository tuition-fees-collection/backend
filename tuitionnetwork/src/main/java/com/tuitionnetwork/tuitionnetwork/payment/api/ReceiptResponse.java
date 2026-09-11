package com.tuitionnetwork.tuitionnetwork.payment.api;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import com.tuitionnetwork.tuitionnetwork.payment.domain.PaymentMethod;
import com.tuitionnetwork.tuitionnetwork.payment.port.in.GetPaymentUseCase.PaymentView;
public record ReceiptResponse(
        String referenceNumber,
        Instant issuedAt,
        BigDecimal amount,
        String currency,
        PaymentMethod method,
        String maskedCardNumber,
        String cardScheme,
        List<ReceiptLine> lines
) {
    public record ReceiptLine(
            String feeType,
            String collectionPeriod,
            BigDecimal amountApplied) {
    }
    public static ReceiptResponse from(PaymentView payment) {
        return new ReceiptResponse(
                payment.receiptReference(),
                payment.settledAt(),
                payment.amount(),
                payment.currency(),
                payment.method(),
                payment.maskedCardNumber(),
                payment.cardScheme(),
                payment.allocations().stream()
                        .map(allocation -> new ReceiptLine(
                        allocation.feeType(),
                        allocation.collectionPeriod(),
                         allocation.amountApplied()))
                        .toList());
    }
}
