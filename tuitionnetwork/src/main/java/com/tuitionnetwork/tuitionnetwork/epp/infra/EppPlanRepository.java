package com.tuitionnetwork.tuitionnetwork.epp.infra;

import com.tuitionnetwork.tuitionnetwork.epp.domain.EppPlan;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface EppPlanRepository extends JpaRepository<EppPlan, UUID> {

    Optional<EppPlan> findByPaymentId(UUID paymentId);

    Optional<EppPlan> findByGatewayPlanId(String gatewayPlanId);
}
