package com.tuitionnetwork.tuitionnetwork.epp.domain;

import com.tuitionnetwork.tuitionnetwork.epp.infra.ports.bankmock.EppPlanResponse;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "epp_plan", schema = "epp")
public class EppPlan {

    @Id
    @GeneratedValue
    private UUID id;


    @Column(name = "payment_id", nullable = false)
    private UUID paymentId;

    @Column(name = "gateway_plan_id")
    private String gatewayPlanId;

    @Column(name = "tenor_months", nullable = false)
    private Integer tenorMonths;

    @Column(nullable = false)
    private BigDecimal principal;

    @Column(nullable = false)
    private String currency;

    @Column(name = "annual_rate")
    private BigDecimal annualRate;

    @Column(name = "interest_amount", nullable = false)
    private BigDecimal interestAmount;

    @Column(name = "admin_fee", nullable = false)
    private BigDecimal adminFee;

    @Column(name = "monthly_instalment", nullable = false)
    private BigDecimal monthlyInstalment;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private EppPlanStatus status;

    @OneToMany(mappedBy = "plan", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("instalmentNumber asc")
    private List<EppInstalment> instalments = new ArrayList<>();

    protected EppPlan() {
        
    }

    
    public static EppPlan fromGatewayResponse(UUID paymentId, EppPlanResponse response) {
        EppPlan plan = new EppPlan();
        plan.paymentId = paymentId;
        plan.gatewayPlanId = response.planId();
        plan.tenorMonths = response.tenorMonths();
        plan.principal = response.principal();
        plan.currency = response.currency();
        plan.annualRate = response.annualRate();
        plan.interestAmount = response.interestAmount();
        plan.adminFee = response.adminFee();
        plan.monthlyInstalment = response.monthlyInstallment();
        plan.status = EppPlanStatus.valueOf(response.status());

        response.schedule().forEach(row -> plan.instalments.add(new EppInstalment(
                plan,
                row.number(),
                LocalDate.parse(row.dueDate()),
                row.amount())));

        return plan;
    }

    public UUID getId() {
        return id;
    }

    public UUID getPaymentId() {
        return paymentId;
    }

    public String getGatewayPlanId() {
        return gatewayPlanId;
    }

    public Integer getTenorMonths() {
        return tenorMonths;
    }

    public BigDecimal getPrincipal() {
        return principal;
    }

    public String getCurrency() {
        return currency;
    }

    public BigDecimal getAnnualRate() {
        return annualRate;
    }

    public BigDecimal getInterestAmount() {
        return interestAmount;
    }

    public BigDecimal getAdminFee() {
        return adminFee;
    }

    public BigDecimal getMonthlyInstalment() {
        return monthlyInstalment;
    }

    public EppPlanStatus getStatus() {
        return status;
    }

    public List<EppInstalment> getInstalments() {
        return Collections.unmodifiableList(instalments);
    }

    public void cancel() {
        if (status == EppPlanStatus.COMPLETED) {
            throw new IllegalStateException("Cannot cancel a completed EPP plan: " + id);
        }
        this.status = EppPlanStatus.CANCELLED;
        instalments.forEach(EppInstalment::markCancelled);
    }
}
