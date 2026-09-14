package com.tuitionnetwork.tuitionnetwork.epp.web;

import com.tuitionnetwork.tuitionnetwork.epp.domain.EppInstalment;
import com.tuitionnetwork.tuitionnetwork.epp.domain.EppPlan;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

// Our own response shape, kept separate from the mock's EppPlanResponse so callers
// of our API aren't coupled to the mock's field names/spelling.
public record EppPlanView(
        UUID id,
        UUID paymentId,
        String gatewayPlanId,
        Integer tenorMonths,
        BigDecimal principal,
        String currency,
        BigDecimal annualRate,
        BigDecimal interestAmount,
        BigDecimal adminFee,
        BigDecimal monthlyInstalment,
        String status,
        List<InstalmentView> instalments) {

    public record InstalmentView(Integer instalmentNumber, LocalDate dueDate, BigDecimal amount, String status) {

        static InstalmentView from(EppInstalment instalment) {
            return new InstalmentView(
                    instalment.getInstalmentNumber(),
                    instalment.getDueDate(),
                    instalment.getAmount(),
                    instalment.getStatus().name());
        }
    }

    public static EppPlanView from(EppPlan plan) {
        return new EppPlanView(
                plan.getId(),
                plan.getPaymentId(),
                plan.getGatewayPlanId(),
                plan.getTenorMonths(),
                plan.getPrincipal(),
                plan.getCurrency(),
                plan.getAnnualRate(),
                plan.getInterestAmount(),
                plan.getAdminFee(),
                plan.getMonthlyInstalment(),
                plan.getStatus().name(),
                plan.getInstalments().stream().map(InstalmentView::from).toList());
    }
}
