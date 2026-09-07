package com.tuitionnetwork.tuitionnetwork.epp.service;

import com.tuitionnetwork.tuitionnetwork.epp.domain.EppPlan;
import com.tuitionnetwork.tuitionnetwork.epp.infra.EppPlanRepository;
import com.tuitionnetwork.tuitionnetwork.epp.infra.ports.EppGatewayPort;
import com.tuitionnetwork.tuitionnetwork.epp.infra.ports.bankmock.EppInstallment;
import com.tuitionnetwork.tuitionnetwork.epp.infra.ports.bankmock.EppPlanResponse;
import com.tuitionnetwork.tuitionnetwork.epp.infra.ports.bankmock.EppQuoteOption;
import com.tuitionnetwork.tuitionnetwork.epp.infra.ports.bankmock.EppQuoteResponse;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class EppServiceTest {

    private final EppGatewayPort gatewayPort = mock(EppGatewayPort.class);
    private final EppPlanRepository repository = mock(EppPlanRepository.class);
    private final EppService service = new EppService(gatewayPort, repository);

    private EppQuoteOption option(int tenor) {
        return new EppQuoteOption(tenor, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO,
                BigDecimal.valueOf(1000), BigDecimal.valueOf(100));
    }

    @Test
    void getQuotesHidesTheTwentyFourMonthOptionOutsideProductScope() {
        EppQuoteResponse mockResponse = new EppQuoteResponse(
                new BigDecimal("24000"), "EGP",
                List.of(option(3), option(6), option(12), option(18), option(24)));
        when(gatewayPort.quote(any(), anyString())).thenReturn(mockResponse);

        EppQuoteResponse result = service.getQuotes(new BigDecimal("24000"), "EGP");

        assertThat(result.options()).extracting(EppQuoteOption::tenorMonths)
                .containsExactlyInAnyOrder(3, 6, 12, 18);
    }

    @Test
    void createPlanRejectsUnsupportedTenor() {
        assertThatThrownBy(() -> service.createPlan(UUID.randomUUID(), "pay-1", 24, "Books", "idem-1"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Unsupported tenor");

        verifyNoInteractions(gatewayPort);
    }

    @Test
    void createPlanRejectsWhenPaymentAlreadyHasAPlan() {
        UUID paymentId = UUID.randomUUID();
        when(repository.findByPaymentId(paymentId)).thenReturn(Optional.of(mock(EppPlan.class)));

        assertThatThrownBy(() -> service.createPlan(paymentId, "pay-1", 12, "Books", "idem-1"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("already has an EPP plan");

        verifyNoInteractions(gatewayPort);
    }

    @Test
    void createPlanMapsGatewayResponseAndSaves() {
        UUID paymentId = UUID.randomUUID();
        when(repository.findByPaymentId(paymentId)).thenReturn(Optional.empty());
        EppPlanResponse gatewayResponse = new EppPlanResponse(
                "plan-1", "ACTIVE", "pay-1", new BigDecimal("6000"), "EGP", 6,
                BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, new BigDecimal("6000"),
                new BigDecimal("1000"), "2026-10-01", "2027-03-01",
                List.of(new EppInstallment(1, "2026-10-01", new BigDecimal("1000"), "DUE")),
                null, null, "Books", "2026-09-07T00:00:00Z", "2026-09-07T00:00:00Z");
        when(gatewayPort.create(any(), anyString())).thenReturn(gatewayResponse);
        when(repository.save(any(EppPlan.class))).thenAnswer(invocation -> invocation.getArgument(0));

        EppPlan plan = service.createPlan(paymentId, "pay-1", 6, "Books", "idem-1");

        assertThat(plan.getGatewayPlanId()).isEqualTo("plan-1");
        assertThat(plan.getPaymentId()).isEqualTo(paymentId);
        verify(repository).save(plan);
    }

    @Test
    void getPlanThrowsNotFoundWhenMissing() {
        UUID planId = UUID.randomUUID();
        when(repository.findById(planId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.getPlan(planId))
                .isInstanceOf(EppPlanNotFoundException.class);
    }
}
