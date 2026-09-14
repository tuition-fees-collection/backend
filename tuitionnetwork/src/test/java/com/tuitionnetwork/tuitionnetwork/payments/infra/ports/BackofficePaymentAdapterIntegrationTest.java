package com.tuitionnetwork.tuitionnetwork.payments.infra.ports;

import com.tuitionnetwork.tuitionnetwork.payments.infra.ports.bankmock.Amount;
import com.tuitionnetwork.tuitionnetwork.payments.infra.ports.bankmock.BackofficePaymentRequest;
import com.tuitionnetwork.tuitionnetwork.payments.infra.ports.bankmock.BackofficePaymentResponse;
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


@Tag("integration")
@ActiveProfiles("test")
@SpringBootTest
class BackofficePaymentAdapterIntegrationTest {

    @Autowired
    private BackofficePaymentAdapter adapter;

    private BackofficePaymentRequest requestFor(String sourceId, BigDecimal amount, boolean capture) {
        return new BackofficePaymentRequest(
                sourceId, new Amount(amount, "EGP"), "BO-" + UUID.randomUUID(), "test payment", capture);
    }

    private BackofficePaymentResponse charge(String sourceId, BigDecimal amount, boolean capture) {
        return adapter.charge(requestFor(sourceId, amount, capture), UUID.randomUUID().toString());
    }

    @Test
    void accountPaymentPostsImmediately() {
        BackofficePaymentResponse response = charge("acc_mona_current", new BigDecimal("5000"), true);

        assertThat(response.status()).isEqualTo("POSTED");
        assertThat(response.approved()).isTrue();
        assertThat(response.source().type()).isEqualTo("ACCOUNT");
        assertThat(response.customerId()).isEqualTo("cif_100001");
    }

    @Test
    void accountPaymentWithInsufficientFundsIsDeclined() {
        assertThatThrownBy(() -> charge("acc_nour_current", new BigDecimal("5000"), true))
                .isInstanceOf(BankMockApiException.class)
                .satisfies(ex -> assertThat(((BankMockApiException) ex).code()).isEqualTo("INSUFFICIENT_FUNDS"));
    }

    @Test
    void accountPaymentFromFrozenAccountIsRejected() {
        assertThatThrownBy(() -> charge("acc_ahmed_current", new BigDecimal("1000"), true))
                .isInstanceOf(BankMockApiException.class)
                .satisfies(ex -> assertThat(((BankMockApiException) ex).code()).isEqualTo("ACCOUNT_NOT_ACTIVE"));
    }

    @Test
    void cardPaymentWithCaptureTrueIsCaptured() {
        BackofficePaymentResponse response = charge("card_mona_visa", new BigDecimal("4500"), true);

        assertThat(response.status()).isEqualTo("CAPTURED");
        assertThat(response.source().type()).isEqualTo("CARD");
    }

    @Test
    void cardPaymentWithCaptureFalseIsAuthorisedThenCaptured() {
        BackofficePaymentResponse authorised = charge("card_mona_visa", new BigDecimal("3000"), false);
        assertThat(authorised.status()).isEqualTo("AUTHORISED");

        BackofficePaymentResponse captured = adapter.capture(authorised.paymentId(), null);
        assertThat(captured.status()).isEqualTo("CAPTURED");
    }

    @Test
    void paymentFromBlockedCardIsRejected() {
        assertThatThrownBy(() -> charge("card_ahmed_visa", new BigDecimal("1000"), true))
                .isInstanceOf(BankMockApiException.class)
                .satisfies(ex -> assertThat(((BankMockApiException) ex).code()).isEqualTo("CARD_BLOCKED"));
    }

    @Test
    void paymentFromUnknownSourceIsNotFound() {
        assertThatThrownBy(() -> charge("acc_does_not_exist", new BigDecimal("1000"), true))
                .isInstanceOf(BankMockApiException.class)
                .satisfies(ex -> assertThat(((BankMockApiException) ex).code()).isEqualTo("SOURCE_NOT_FOUND"));
    }

    @Test
    void statusCanBeFetchedByPaymentId() {
        BackofficePaymentResponse created = charge("acc_mona_savings", new BigDecimal("1000"), true);

        BackofficePaymentResponse fetched = adapter.getStatus(created.paymentId());

        assertThat(fetched.paymentId()).isEqualTo(created.paymentId());
        assertThat(fetched.status()).isEqualTo("POSTED");
    }

    @Test
    void listFiltersByCustomerId() {
        BackofficePaymentResponse created = charge("acc_mona_current", new BigDecimal("500"), true);

        var payments = adapter.list("cif_100001", null, null);

        assertThat(payments).extracting(BackofficePaymentResponse::paymentId).contains(created.paymentId());
    }
}
