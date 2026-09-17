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
            boolean idempotentReplay,

            /**
             * What this order will pay, fee by fee.
             *
             * Echoed back so a client can confirm the order it just created
             * without a second call - and so it can see the fee names, which it
             * never sent. It posted ids and amounts; billing supplied the rest.
             *
             * The amount is the sum of these lines and is checked against them
             * before anything is saved, so a client cannot post a total that
             * disagrees with its own breakdown.
             */
            List<AllocatedLine> allocations
    ) {
    }

    /**
     * One fee line of an order, as billing describes it.
     *
     * feeType and collectionPeriod come from the fee records already loaded to
     * validate the request - no extra call to billing to fill them in.
     */
    record AllocatedLine(
            UUID feeId,
            String feeType,
            String collectionPeriod,
            BigDecimal amountApplied
    ) {
    }
}