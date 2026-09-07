package com.tuitionnetwork.tuitionnetwork.epp.service;

import com.tuitionnetwork.tuitionnetwork.epp.domain.EppPlan;
import com.tuitionnetwork.tuitionnetwork.epp.infra.EppPlanRepository;
import com.tuitionnetwork.tuitionnetwork.epp.infra.ports.EppGatewayPort;
import com.tuitionnetwork.tuitionnetwork.epp.infra.ports.bankmock.EppCreateRequest;
import com.tuitionnetwork.tuitionnetwork.epp.infra.ports.bankmock.EppPlanResponse;
import com.tuitionnetwork.tuitionnetwork.epp.infra.ports.bankmock.EppQuoteResponse;
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

    public EppService(EppGatewayPort eppGatewayPort, EppPlanRepository eppPlanRepository) {
        this.eppGatewayPort = eppGatewayPort;
        this.eppPlanRepository = eppPlanRepository;
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
    public EppPlan createPlan(UUID paymentId, String gatewayPaymentId, int tenorMonths,
                               String productName, String idempotencyKey) {
        if (!SUPPORTED_TENORS.contains(tenorMonths)) {
            throw new IllegalArgumentException(
                    "Unsupported tenor: " + tenorMonths + " (allowed: " + SUPPORTED_TENORS + ")");
        }
        if (eppPlanRepository.findByPaymentId(paymentId).isPresent()) {
            throw new IllegalStateException("Payment " + paymentId + " already has an EPP plan");
        }

        EppCreateRequest request = new EppCreateRequest(
                tenorMonths, gatewayPaymentId, null, null, null, productName);
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

    public List<EppPlan> findAll() {
        return eppPlanRepository.findAll();
    }

    public EppPlan getPlan(UUID planId) {
        return eppPlanRepository.findById(planId)
                .orElseThrow(() -> new EppPlanNotFoundException(planId));
    }
}
