package com.tuitionnetwork.tuitionnetwork.payment.services;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.json.JsonMapper;
import com.tuitionnetwork.tuitionnetwork.identity.service.IdentityAccessService;
import com.tuitionnetwork.tuitionnetwork.identity.service.PaymentCaller;
import com.tuitionnetwork.tuitionnetwork.payment.domain.OutboxEvent;
import com.tuitionnetwork.tuitionnetwork.payment.domain.Payment;
import com.tuitionnetwork.tuitionnetwork.payment.domain.PaymentAllocation;
import com.tuitionnetwork.tuitionnetwork.payment.domain.PaymentMethod;
import com.tuitionnetwork.tuitionnetwork.payment.domain.PaymentType;
import com.tuitionnetwork.tuitionnetwork.payment.domain.event.PaymentCreatedEvent;
import com.tuitionnetwork.tuitionnetwork.payment.port.in.CreatePaymentUseCase;
import com.tuitionnetwork.tuitionnetwork.payment.port.portout.BillingPort;
import com.tuitionnetwork.tuitionnetwork.payment.port.portout.OutboxEventPort;
import com.tuitionnetwork.tuitionnetwork.payment.port.portout.PaymentAllocationRepository;
import com.tuitionnetwork.tuitionnetwork.payment.port.portout.PaymentRepository;

@Service
public class PaymentService implements CreatePaymentUseCase {
    private static final Logger log = LoggerFactory.getLogger(PaymentService.class);
    private final PaymentRepository payments;
    private final PaymentAllocationRepository allocations;
    private final BillingPort billing;
    private final IdentityAccessService identity;
    private final OutboxEventPort outbox;
    private final JsonMapper json;

    public PaymentService(PaymentRepository payments,
                          PaymentAllocationRepository allocations,
                          BillingPort billing,
                          IdentityAccessService identity,
                          OutboxEventPort outbox,
                          JsonMapper json) {
        this.payments = payments;
        this.allocations = allocations;
        this.billing = billing;
        this.identity = identity;
        this.outbox = outbox;
        this.json = json;
    }

    @Override
    @Transactional
    public PaymentOrderView create(CreatePaymentCommand command) {
        PaymentCaller caller = callerFor(command.studentId());
        if (command.method() == PaymentMethod.CASH) {
            identity.requireCashCollection();
        }
        if (command.paymentType() == PaymentType.EPP
                && command.method() != PaymentMethod.CREDIT_CARD) {
            throw new Paymentvalidationexception("EPP_REQUIRES_CREDIT_CARD",
                    "Installments are available only for credit-card payments");
        }

        Optional<Payment> existing =
                payments.findByGuardianIdAndIdempotencyKey(caller.guardianId(), command.idempotencyKey());
        if (existing.isPresent()) {
            log.info("Idempotent replay of key {} for guardian {}",
                    command.idempotencyKey(), caller.guardianId());
            List<CreatePaymentUseCase.AllocatedLine> replayed =
                    allocations.findByPaymentId(existing.get().getId()).stream()
                            .map(row -> new CreatePaymentUseCase.AllocatedLine(
                                    row.getFeeId(), null, null, row.getAmountApplied()))
                            .toList();
            return view(existing.get(), true, replayed);
        }

        List<UUID> feeIds = command.feeLines().stream().map(line -> line.feeId()).toList();
        if (new HashSet<>(feeIds).size() != feeIds.size()) {
            throw new Paymentvalidationexception("DUPLICATE_FEE_LINE",
                    "The same fee appears more than once in this payment");
        }

        Map<UUID, BillingPort.PayableFee> fees = billing.loadPayable(feeIds).stream()
                .collect(Collectors.toMap(fee -> fee.feeId(), Function.identity()));

        for (UUID feeId : feeIds) {
            BillingPort.PayableFee fee = fees.get(feeId);
            if (fee == null || !command.studentId().equals(fee.studentId())) {
                throw new PaymentAccessDeniedException(caller.guardianId(), feeId);
            }
        }

        for (FeeLine line : command.feeLines()) {
            BillingPort.PayableFee fee = fees.get(line.feeId());
            if (!fee.isPayable()) {
                throw new Paymentvalidationexception("FEE_NOT_PAYABLE",
                        "This fee is no longer outstanding");
            }
            if (line.amountToApply().compareTo(fee.outstanding()) > 0) {
                throw new Paymentvalidationexception("AMOUNT_EXCEEDS_OUTSTANDING",
                        "Cannot pay more than is owed on a fee line");
            }
            if (command.paymentType() == PaymentType.FULL
                    && line.amountToApply().compareTo(fee.outstanding()) != 0) {
                throw new Paymentvalidationexception("PARTIAL_AMOUNT_ON_FULL_PAYMENT",
                        "A full payment must clear the whole outstanding balance of every fee line");
            }
        }

        BigDecimal total = command.totalAmount();
        Payment payment = payments.save(Payment.create(
                command.idempotencyKey(),
                caller.guardianId(),
                caller.processedBy(),
                command.method(),
                command.paymentType(),
                total,
                command.currency()));

        List<PaymentAllocation> rows = new ArrayList<>();
        BigDecimal allocated = BigDecimal.ZERO;
        for (FeeLine line : command.feeLines()) {
            rows.add(PaymentAllocation.of(payment.getId(), line.feeId(), line.amountToApply()));
            allocated = allocated.add(line.amountToApply());
        }
        if (allocated.compareTo(total) != 0) {
            throw new Paymentvalidationexception("ALLOCATION_MISMATCH",
                    "Allocations total " + allocated + " but the payment is " + total);
        }
        allocations.saveAll(rows);
        outbox.save(OutboxEvent.of(payment.getId(), PaymentCreatedEvent.EVENT_TYPE,
                serialise(new PaymentCreatedEvent(
                        payment.getId(),
                        payment.getGuardianId(),
                        payment.getAmount(),
                        payment.getCurrency(),
                        payment.getMethod(),
                        payment.getPaymentType(),
                        payment.getCreatedAt(),
                        payment.getExpiresAt()))));

        log.info("Created payment {} for guardian {} totalling {} {}",
                payment.getId(), payment.getGuardianId(), total, payment.getCurrency());
        List<CreatePaymentUseCase.AllocatedLine> lines = command.feeLines().stream()
                .map(line -> {
                    BillingPort.PayableFee fee = fees.get(line.feeId());
                    return new CreatePaymentUseCase.AllocatedLine(
                            line.feeId(), fee.feeType(), fee.collectionPeriod(),
                            line.amountToApply());
                })
                .toList();

        return view(payment, false, lines);
    }

    private PaymentCaller callerFor(UUID studentId) {
        try {
            return identity.authorizePaymentCreation(studentId);
        } catch (AccessDeniedException e) {
            throw new PaymentAccessDeniedException(studentId, studentId);
        }
    }

    private PaymentOrderView view(Payment payment, boolean replay,
                                  List<CreatePaymentUseCase.AllocatedLine> lines) {
        return new PaymentOrderView(
                payment.getId(),
                payment.getStatus(),
                payment.getAmount(),
                payment.getCurrency(),
                payment.getMethod(),
                payment.getPaymentType(),
                payment.getCreatedAt(),
                payment.getExpiresAt(),
                replay,
                lines == null ? List.of() : lines);
    }

    private String serialise(Object event) {
        try {
            return json.writeValueAsString(event);
        } catch (Exception e) {
            throw new IllegalStateException("Could not serialise outbox payload", e);
        }
    }
}