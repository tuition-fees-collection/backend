package com.tuitionnetwork.tuitionnetwork.epp.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "epp_instalment", schema = "epp")
public class EppInstalment {

    @Id
    @GeneratedValue
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "plan_id", nullable = false)
    private EppPlan plan;

    @Column(name = "instalment_number", nullable = false)
    private Integer instalmentNumber;

    @Column(name = "due_date", nullable = false)
    private LocalDate dueDate;

    @Column(nullable = false)
    private BigDecimal amount;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private EppInstalmentStatus status;

    protected EppInstalment() {
        
    }

    EppInstalment(EppPlan plan, Integer instalmentNumber, LocalDate dueDate, BigDecimal amount) {
        this.plan = plan;
        this.instalmentNumber = instalmentNumber;
        this.dueDate = dueDate;
        this.amount = amount;
        this.status = EppInstalmentStatus.DUE;
    }

    public UUID getId() {
        return id;
    }

    public Integer getInstalmentNumber() {
        return instalmentNumber;
    }

    public LocalDate getDueDate() {
        return dueDate;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public EppInstalmentStatus getStatus() {
        return status;
    }

    void markCancelled() {
        this.status = EppInstalmentStatus.CANCELLED;
    }
}
