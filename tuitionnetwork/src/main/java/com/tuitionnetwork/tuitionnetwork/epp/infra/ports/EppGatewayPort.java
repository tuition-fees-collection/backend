package com.tuitionnetwork.tuitionnetwork.epp.infra.ports;

import com.tuitionnetwork.tuitionnetwork.epp.infra.ports.bankmock.EppCreateRequest;
import com.tuitionnetwork.tuitionnetwork.epp.infra.ports.bankmock.EppPlanResponse;
import com.tuitionnetwork.tuitionnetwork.epp.infra.ports.bankmock.EppQuoteResponse;

import java.math.BigDecimal;

public interface EppGatewayPort {

    EppQuoteResponse quote(BigDecimal amount, String currency);

    EppPlanResponse create(EppCreateRequest request, String idempotencyKey);

    EppPlanResponse cancel(String gatewayPlanId);
}
