package com.tuitionnetwork.tuitionnetwork.payment.services;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.tuitionnetwork.tuitionnetwork.payment.domain.Payment;
import com.tuitionnetwork.tuitionnetwork.payment.domain.PaymentAllocation;
import com.tuitionnetwork.tuitionnetwork.payment.port.in.GetPaymentUseCase;
import com.tuitionnetwork.tuitionnetwork.payment.port.portout.BillingPort;
import com.tuitionnetwork.tuitionnetwork.payment.port.portout.PaymentAllocationRepository;
import com.tuitionnetwork.tuitionnetwork.payment.port.portout.PaymentRepository;
import com.tuitionnetwork.tuitionnetwork.payment.port.portout.ReceiptPort;
@Service
public class PaymentQueryService implements GetPaymentUseCase {

    private final PaymentRepository payments;
    private final PaymentAllocationRepository allocations;
    private final BillingPort billing;
    private final ReceiptPort receipts;

    public PaymentQueryService(PaymentRepository payments,
                               PaymentAllocationRepository allocations,
                               BillingPort billing,
                               ReceiptPort receipts) {
        this.payments = payments;
        this.allocations = allocations;
        this.billing = billing;
        this.receipts = receipts;
    }

    @Override
    @Transactional(readOnly = true)
    public PaymentView getById(UUID paymentId, UUID requestingGuardianId) {
        Payment payment = payments.findById(paymentId)
                .orElseThrow(() -> new PaymentAccessDeniedException(requestingGuardianId, paymentId));
        if (!payment.getGuardianId().equals(requestingGuardianId)) {
            throw new PaymentAccessDeniedException(requestingGuardianId, paymentId);
        }

        List<PaymentAllocation> rows = allocations.findByPaymentId(paymentId);
        Map<UUID, BillingPort.PayableFee> fees = billing
                .loadPayable(rows.stream().map(PaymentAllocation::getFeeId).toList())
                .stream()
                .collect(Collectors.toMap(BillingPort.PayableFee::feeId, Function.identity()));

        List<AllocationView> allocationViews = rows.stream()
                .map(row -> {
                    BillingPort.PayableFee fee = fees.get(row.getFeeId());
                    return new AllocationView(
                            row.getFeeId(),
                            fee == null ? null : fee.feeType(),
                            fee == null ? null : fee.collectionPeriod(),
                            row.getAmountApplied());
                })
                .toList();

        String receiptReference = receipts.findByPaymentId(paymentId)
                .map(ReceiptPort.IssuedReceipt::referenceNumber)
                .orElse(null);

        return new PaymentView(
                payment.getId(),
                payment.getStatus(),
                payment.getAmount(),
                payment.getCurrency(),
                payment.getMethod(),
                payment.getPaymentType(),
                payment.getMaskedCardNumber(),
                payment.getCardScheme(),
                payment.getCreatedAt(),
                payment.getSettledAt(),
                receiptReference,
                allocationViews);
    }

    @Override
    @Transactional(readOnly = true)
    public List<PaymentSummary> history(UUID guardianId) {
        return payments.findByGuardianIdOrderByCreatedAtDesc(guardianId).stream()
                .map(payment -> new PaymentSummary(
                        payment.getId(),
                        payment.getStatus(),
                        payment.getAmount(),
                        payment.getCurrency(),
                        payment.getMethod(),
                        payment.getPaymentType(),
                        payment.getMaskedCardNumber(),
                        payment.getCardScheme(),
                        payment.getCreatedAt(),
                        payment.getSettledAt(),
                        receiptReferenceFor(payment)))
                .toList();
    }
    private String receiptReferenceFor(Payment payment) {
        if (!payment.isReceiptEligible()) {
            return null;
        }
        return receipts.findByPaymentId(payment.getId())
                .map(ReceiptPort.IssuedReceipt::referenceNumber)
                .orElse(null);
    }
}
