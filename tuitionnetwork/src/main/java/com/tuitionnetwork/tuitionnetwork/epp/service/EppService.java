package com.tuitionnetwork.tuitionnetwork.epp.service;

import com.tuitionnetwork.tuitionnetwork.epp.domain.EppPlan;
import com.tuitionnetwork.tuitionnetwork.epp.infra.EppPlanRepository;
import com.tuitionnetwork.tuitionnetwork.epp.infra.ports.EppGatewayPort;
import com.tuitionnetwork.tuitionnetwork.epp.infra.ports.bankmock.EppCreateRequest;
import com.tuitionnetwork.tuitionnetwork.epp.infra.ports.bankmock.EppPlanResponse;
import com.tuitionnetwork.tuitionnetwork.epp.infra.ports.bankmock.EppQuoteResponse;
import com.tuitionnetwork.tuitionnetwork.payments.infra.ports.bankmock.Amount;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@Service
public class EppService {


    private static final Set<Integer> SUPPORTED_TENORS = Set.of(3, 6, 12, 18);

    private final EppGatewayPort eppGatewayPort;
    private final EppPlanRepository eppPlanRepository;
    private final EppPaymentPort eppPaymentPort;

    public EppService(EppGatewayPort eppGatewayPort, EppPlanRepository eppPlanRepository,
                      EppPaymentPort eppPaymentPort) {
        this.eppGatewayPort = eppGatewayPort;
        this.eppPlanRepository = eppPlanRepository;
        this.eppPaymentPort = eppPaymentPort;
    }

    public EppQuoteResponse getQuotes(BigDecimal amount, String currency) {
        EppQuoteResponse response = eppGatewayPort.quote(amount, currency);
        return new EppQuoteResponse(
                response.amount(),
                response.currency(),
                response.options().stream()
                        .filter(option -> SUPPORTED_TENORS.contains(option.tenorMonths()))
                        .toList());
    }

    @Transactional
    public EppPlan createPlan(UUID paymentId, int tenorMonths,
                               String productName, String idempotencyKey) {
        if (!SUPPORTED_TENORS.contains(tenorMonths)) {
            throw new IllegalArgumentException(
                    "Unsupported tenor: " + tenorMonths + " (allowed: " + SUPPORTED_TENORS + ")");
        }
        if (eppPlanRepository.findByPaymentId(paymentId).isPresent()) {
            throw new IllegalStateException("Payment " + paymentId + " already has an EPP plan");
        }

        EppPaymentPort.EligiblePayment payment = eppPaymentPort.requireEligiblePayment(paymentId);

        EppCreateRequest request = new EppCreateRequest(
                tenorMonths, payment.gatewayPaymentId(), null,
                new Amount(payment.amount(), payment.currency()), null, productName);
        EppPlanResponse response = eppGatewayPort.create(request, idempotencyKey);

        EppPlan plan = EppPlan.fromGatewayResponse(paymentId, response);
        return eppPlanRepository.save(plan);
    }

    @Transactional
    public EppPlan cancelPlan(UUID planId) {
        EppPlan plan = getPlan(planId);

        eppGatewayPort.cancel(plan.getGatewayPlanId());
        plan.cancel();
        return plan;
    }

    @Transactional(readOnly = true)
    public List<EppPlan> findAll() {
        List<EppPlan> plans = eppPlanRepository.findAll();
        plans.forEach(plan -> plan.getInstalments().size());
        return plans;
    }

    @Transactional(readOnly = true)
    public EppPlan getPlan(UUID planId) {
        EppPlan plan = eppPlanRepository.findById(planId)
                .orElseThrow(() -> new EppPlanNotFoundException(planId));
        plan.getInstalments().size();
        return plan;
    }
}
