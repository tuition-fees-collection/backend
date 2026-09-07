package com.tuitionnetwork.tuitionnetwork.payments.infra.ports;

import com.tuitionnetwork.tuitionnetwork.payments.infra.ports.bankmock.Amount;
import com.tuitionnetwork.tuitionnetwork.payments.infra.ports.bankmock.CardDetails;
import com.tuitionnetwork.tuitionnetwork.payments.infra.ports.bankmock.CardPaymentRequest;
import com.tuitionnetwork.tuitionnetwork.payments.infra.ports.bankmock.CardPaymentResponse;
import com.tuitionnetwork.tuitionnetwork.payments.infra.ports.bankmock.BankMockApiException;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.math.BigDecimal;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

// these call the mock bank service on localhost:8000, so make
// sure it's running before you try these locally.
@Tag("integration")
@SpringBootTest
class CardGatewayAdapterIntegrationTest {

    @Autowired
    private CardGatewayAdapter adapter;

    private CardPaymentRequest requestWith(String cardNumber, BigDecimal amount) {
        return new CardPaymentRequest(
                new CardDetails(cardNumber, "MONA SAMIR", 12, 2030, "123"),
                new Amount(amount, "EGP"),
                "ORD-" + UUID.randomUUID(),
                null,
                null,
                true);
    }

    private CardPaymentResponse charge(String cardNumber, BigDecimal amount) {
        return adapter.charge(requestWith(cardNumber, amount), UUID.randomUUID().toString());
    }

    @Test
    void approvedVisaIsCaptured() {
        CardPaymentResponse response = charge("4111111111111111", new BigDecimal("4500"));

        assertThat(response.status()).isEqualTo("CAPTURED");
        assertThat(response.approved()).isTrue();
        assertThat(response.card().scheme()).isEqualToIgnoringCase("VISA");
    }

    @Test
    void insufficientFundsIsDeclined() {
        assertThatThrownBy(() -> charge("4000000000000002", new BigDecimal("1000")))
                .isInstanceOf(BankMockApiException.class)
                .satisfies(ex -> {
                    BankMockApiException e = (BankMockApiException) ex;
                    assertThat(e.code()).isEqualTo("CARD_DECLINED");
                    assertThat(e.details()).containsEntry("response_code", "51");
                });
    }

    @Test
    void doNotHonourIsDeclined() {
        assertThatThrownBy(() -> charge("4000000000009995", new BigDecimal("1000")))
                .isInstanceOf(BankMockApiException.class)
                .satisfies(ex -> assertThat(((BankMockApiException) ex).details())
                        .containsEntry("response_code", "05"));
    }

    @Test
    void incorrectCvvIsDeclined() {
        assertThatThrownBy(() -> charge("4000000000000101", new BigDecimal("1000")))
                .isInstanceOf(BankMockApiException.class)
                .satisfies(ex -> assertThat(((BankMockApiException) ex).details())
                        .containsEntry("response_code", "82"));
    }

    @Test
    void expiredCardIsDeclined() {
        assertThatThrownBy(() -> charge("4000000000000069", new BigDecimal("1000")))
                .isInstanceOf(BankMockApiException.class)
                .satisfies(ex -> assertThat(((BankMockApiException) ex).details())
                        .containsEntry("response_code", "54"));
    }

    @Test
    void issuerUnavailableIsRetryable() {
        assertThatThrownBy(() -> charge("4000000000000119", new BigDecimal("1000")))
                .isInstanceOf(BankMockApiException.class)
                .satisfies(ex -> assertThat(((BankMockApiException) ex).code()).isEqualTo("ISSUER_UNAVAILABLE"));
    }

    @Test
    void threeDSCardReturnsPendingNotAnException() {
        CardPaymentResponse response = charge("4000000000003220", new BigDecimal("1000"));

        assertThat(response.status()).isEqualTo("PENDING_3DS");
        assertThat(response.approved()).isFalse();
    }

    @Test
    void confirming3DSWithCorrectOtpApprovesThePayment() {
        CardPaymentResponse pending = charge("4000000000003220", new BigDecimal("1000"));

        CardPaymentResponse confirmed = adapter.confirm3ds(pending.paymentId(), "123456");

        assertThat(confirmed.status()).isIn("CAPTURED", "AUTHORISED");
    }

    @Test
    void confirming3DSWithWrongOtpFails() {
        CardPaymentResponse pending = charge("4000000000003220", new BigDecimal("1000"));

        assertThatThrownBy(() -> adapter.confirm3ds(pending.paymentId(), "000000"))
                .isInstanceOf(BankMockApiException.class);
    }

    @Test
    void amountLimitedCardApprovesUnderLimit() {
        CardPaymentResponse response = charge("4000000000000077", new BigDecimal("5000"));

        assertThat(response.status()).isEqualTo("CAPTURED");
    }

    @Test
    void amountLimitedCardDeclinesOverLimit() {
        assertThatThrownBy(() -> charge("4000000000000077", new BigDecimal("15000")))
                .isInstanceOf(BankMockApiException.class)
                .satisfies(ex -> assertThat(((BankMockApiException) ex).code()).isEqualTo("CARD_DECLINED"));
    }
}
