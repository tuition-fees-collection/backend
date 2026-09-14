package com.tuitionnetwork.tuitionnetwork.epp.infra.ports;

import com.tuitionnetwork.tuitionnetwork.epp.infra.ports.bankmock.EppCreateRequest;
import com.tuitionnetwork.tuitionnetwork.epp.infra.ports.bankmock.EppPlanResponse;
import com.tuitionnetwork.tuitionnetwork.epp.infra.ports.bankmock.EppQuoteResponse;
import com.tuitionnetwork.tuitionnetwork.payments.infra.ports.CardGatewayAdapter;
import com.tuitionnetwork.tuitionnetwork.payments.infra.ports.bankmock.Amount;
import com.tuitionnetwork.tuitionnetwork.payments.infra.ports.bankmock.CardDetails;
import com.tuitionnetwork.tuitionnetwork.payments.infra.ports.bankmock.CardPaymentRequest;
import com.tuitionnetwork.tuitionnetwork.payments.infra.ports.bankmock.CardPaymentResponse;
import com.tuitionnetwork.tuitionnetwork.payments.infra.ports.bankmock.BankMockApiException;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

// these call the mock bank service on localhost:8000, so make
// sure it's running before you try these locally.
@Tag("integration")
@ActiveProfiles("test")
@SpringBootTest
class EppGatewayAdapterIntegrationTest {

    @Autowired
    private EppGatewayAdapter eppAdapter;

    @Autowired
    private CardGatewayAdapter cardAdapter;

    private String chargeAndGetPaymentId(String cardNumber, BigDecimal amount) {
        var request = new CardPaymentRequest(
                new CardDetails(cardNumber, "MONA SAMIR", 12, 2030, "123"),
                new Amount(amount, "EGP"),
                "ORD-" + UUID.randomUUID(),
                null,
                null,
                true);
        CardPaymentResponse response = cardAdapter.charge(request, UUID.randomUUID().toString());
        return response.paymentId();
    }

    @Test
    void quoteReturnsAllProductTenors() {
        EppQuoteResponse response = eppAdapter.quote(new BigDecimal("24000"), "EGP");

        assertThat(response.options())
                .extracting(option -> option.tenorMonths())
                .contains(3, 6, 12, 18);
    }

    @Test
    void creatingPlanFromApprovedCreditCardPaymentSucceeds() {
        String paymentId = chargeAndGetPaymentId("4111111111111111", new BigDecimal("24000"));

        var request = new EppCreateRequest(12, paymentId, null, null, null, "Laptop");
        EppPlanResponse plan = eppAdapter.create(request, UUID.randomUUID().toString());

        assertThat(plan.status()).isEqualTo("ACTIVE");
        assertThat(plan.tenorMonths()).isEqualTo(12);
        assertThat(plan.schedule()).hasSize(12);
    }

    @Test
    void creatingPlanFromDebitCardPaymentIsRejected() {
        // 4000056655665556 is approved, but as a debit card — EPP must reject it.
        String paymentId = chargeAndGetPaymentId("4000056655665556", new BigDecimal("24000"));

        var request = new EppCreateRequest(12, paymentId, null, null, null, "Laptop");

        assertThatThrownBy(() -> eppAdapter.create(request, UUID.randomUUID().toString()))
                .isInstanceOf(BankMockApiException.class)
                .satisfies(ex -> assertThat(((BankMockApiException) ex).code()).isEqualTo("CARD_NOT_ELIGIBLE"));
    }

    @Test
    void cancellingAPlanSetsStatusCancelled() {
        String paymentId = chargeAndGetPaymentId("4111111111111111", new BigDecimal("6000"));
        EppPlanResponse plan = eppAdapter.create(
                new EppCreateRequest(6, paymentId, null, null, null, "Books"),
                UUID.randomUUID().toString());

        EppPlanResponse cancelled = eppAdapter.cancel(plan.planId());

        assertThat(cancelled.status()).isEqualTo("CANCELLED");
    }
}
