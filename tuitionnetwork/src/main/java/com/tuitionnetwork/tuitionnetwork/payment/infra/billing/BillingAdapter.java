package com.tuitionnetwork.tuitionnetwork.payment.infra.billing;

import java.util.List;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import com.tuitionnetwork.tuitionnetwork.billing.domain.Fee;
import com.tuitionnetwork.tuitionnetwork.billing.service.BillingService;
import com.tuitionnetwork.tuitionnetwork.billing.service.Concurrentsettlementexception;
import com.tuitionnetwork.tuitionnetwork.payment.port.portout.BillingPort;

@Component
public class BillingAdapter implements BillingPort {

    private static final Logger log = LoggerFactory.getLogger(BillingAdapter.class);

    private final BillingService billing;

    public BillingAdapter(BillingService billing) {
        this.billing = billing;
    }

    @Override
    @Transactional(readOnly = true)
    public List<PayableFee> loadPayable(List<UUID> feeIds) {
        if (feeIds == null || feeIds.isEmpty()) {
            return List.of();
        }
        return billing.findAllById(feeIds).stream()
                .map(BillingAdapter::toPayableFee)
                .toList();
    }

    @Override
    @Transactional
    public void settle(UUID paymentId, List<FeeSettlement> settlements) {
        if (settlements == null || settlements.isEmpty()) {
            return;
        }

        try {
            for (FeeSettlement settlement : settlements) {
                billing.settle(settlement.feeId(), settlement.amountApplied(), settlement.expectedVersion());
            }
            log.info("Payment {} settled {} fee line(s)", paymentId, settlements.size());
        } catch (Concurrentsettlementexception e) {
            log.warn("Concurrency conflict during settlement for payment {}: {}", paymentId, e.getMessage());
            throw e;
        } catch (OptimisticLockingFailureException e) {
            // Safety net: a race that slips past the explicit version check
            // (e.g. two threads both pass it before either flushes) still
            // surfaces as a plain Hibernate optimistic-lock failure here.
            // Normalize it to the same message callers rely on.
            log.warn("Concurrency conflict during settlement for payment {}: {}", paymentId, e.getMessage());
            throw new Concurrentsettlementexception(
                "Fee settlement for payment " + paymentId + " was modified by another payment", e);
        }
    }

    private static PayableFee toPayableFee(Fee fee) {
        return new PayableFee(
                fee.getId(),
                fee.getStudentId(),
                fee.getInstitutionId(),
                fee.getFeeType() == null ? null : fee.getFeeType().name(),
                fee.getCollectionPeriod(),
                fee.getAmount(),
                fee.getAmountPaid(),
                fee.getStatus() == null ? null : fee.getStatus().name(),
                fee.getVersion() == null ? 0 : fee.getVersion());
    }
}