package com.tuitionnetwork.tuitionnetwork.epp.domain;

import com.tuitionnetwork.tuitionnetwork.epp.infra.ports.bankmock.EppInstallment;
import com.tuitionnetwork.tuitionnetwork.epp.infra.ports.bankmock.EppPlanResponse;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class EppPlanTest {

    private EppPlanResponse sampleResponse() {
        return new EppPlanResponse(
                "plan-123",
                "ACTIVE",
                "pay-456",
                new BigDecimal("24000.00"),
                "EGP",
                12,
                new BigDecimal("0.00"),
                new BigDecimal("0.00"),
                new BigDecimal("0.00"),
                new BigDecimal("24000.00"),
                new BigDecimal("2000.00"),
                "2026-10-01",
                "2027-09-01",
                List.of(
                        new EppInstallment(1, "2026-10-01", new BigDecimal("2000.00"), "DUE"),
                        new EppInstallment(2, "2026-11-01", new BigDecimal("2000.00"), "DUE")),
                null,
                null,
                "Laptop",
                "2026-09-07T00:00:00Z",
                "2026-09-07T00:00:00Z");
    }

    @Test
    void fromGatewayResponseMapsMonthlyInstalmentCorrectly() {
        UUID paymentId = UUID.randomUUID();

        EppPlan plan = EppPlan.fromGatewayResponse(paymentId, sampleResponse());

        assertThat(plan.getPaymentId()).isEqualTo(paymentId);
        assertThat(plan.getGatewayPlanId()).isEqualTo("plan-123");
        assertThat(plan.getMonthlyInstalment()).isEqualByComparingTo("2000.00");
        assertThat(plan.getStatus()).isEqualTo(EppPlanStatus.ACTIVE);
    }

    @Test
    void fromGatewayResponseBuildsInstalmentScheduleInOrder() {
        EppPlan plan = EppPlan.fromGatewayResponse(UUID.randomUUID(), sampleResponse());

        assertThat(plan.getInstalments()).hasSize(2);
        assertThat(plan.getInstalments().get(0).getInstalmentNumber()).isEqualTo(1);
        assertThat(plan.getInstalments().get(0).getStatus()).isEqualTo(EppInstalmentStatus.DUE);
        assertThat(plan.getInstalments().get(1).getInstalmentNumber()).isEqualTo(2);
    }

    @Test
    void cancelMarksPlanAndAllInstalmentsCancelled() {
        EppPlan plan = EppPlan.fromGatewayResponse(UUID.randomUUID(), sampleResponse());

        plan.cancel();

        assertThat(plan.getStatus()).isEqualTo(EppPlanStatus.CANCELLED);
        assertThat(plan.getInstalments()).allSatisfy(
                instalment -> assertThat(instalment.getStatus()).isEqualTo(EppInstalmentStatus.CANCELLED));
    }

    @Test
    void cancelOnCompletedPlanThrows() {
        EppPlanResponse completedResponse = new EppPlanResponse(
                "plan-999", "COMPLETED", "pay-456", new BigDecimal("6000.00"), "EGP", 6,
                new BigDecimal("0.00"), new BigDecimal("0.00"), new BigDecimal("0.00"),
                new BigDecimal("6000.00"), new BigDecimal("1000.00"),
                "2026-10-01", "2027-03-01", List.of(), null, null, "Books",
                "2026-09-07T00:00:00Z", "2026-09-07T00:00:00Z");
        EppPlan plan = EppPlan.fromGatewayResponse(UUID.randomUUID(), completedResponse);

        assertThatThrownBy(plan::cancel).isInstanceOf(IllegalStateException.class);
    }
}
