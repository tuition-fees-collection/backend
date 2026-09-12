package com.tuitionnetwork.tuitionnetwork.payment.port.in;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import com.tuitionnetwork.tuitionnetwork.payment.domain.PaymentMethod;
import com.tuitionnetwork.tuitionnetwork.payment.domain.PaymentStatus;
import com.tuitionnetwork.tuitionnetwork.payment.domain.PaymentType;
public interface CreatePaymentUseCase {
    PaymentOrderView create(CreatePaymentCommand command);
     record CreatePaymentCommand(
            String idempotencyKey,
            UUID studentId,
            List<FeeLine> feeLines,
            PaymentMethod method,
            PaymentType paymentType,
            String currency
    ) {
        public CreatePaymentCommand {
            if (feeLines == null || feeLines.isEmpty()) {
                throw new IllegalArgumentException("A payment must settle at least one fee line");
            }
            feeLines = List.copyOf(feeLines);
        }
        public BigDecimal totalAmount() {
            return feeLines.stream()
                    .map(FeeLine::amountToApply)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
        }
    }

    record FeeLine(UUID feeId, BigDecimal amountToApply) {
        public FeeLine {
            if (amountToApply == null || amountToApply.signum() <= 0) {
                throw new IllegalArgumentException("amountToApply must be greater than zero");
            }
        }
    }
    record PaymentOrderView(
            UUID paymentId,
            PaymentStatus status,
            BigDecimal amount,
            String currency,
            PaymentMethod method,
            PaymentType paymentType,
            Instant createdAt,
            Instant expiresAt,
            boolean idempotentReplay
    ) {
    }
}
