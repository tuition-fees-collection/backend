package com.tuitionnetwork.tuitionnetwork.payment.infra.gateway;

import com.tuitionnetwork.tuitionnetwork.payment.port.portout.CardGatewayPort;
import com.tuitionnetwork.tuitionnetwork.payments.infra.ports.bankmock.BankMockApiException;
import com.tuitionnetwork.tuitionnetwork.payments.infra.ports.bankmock.CardPaymentResponse;
import com.tuitionnetwork.tuitionnetwork.payments.infra.ports.bankmock.CardSummary;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class DevDCardGatewayBridgeTest {

    private final com.tuitionnetwork.tuitionnetwork.payments.infra.ports.CardGatewayPort delegate =
            mock(com.tuitionnetwork.tuitionnetwork.payments.infra.ports.CardGatewayPort.class);
    private final DevDCardGatewayBridge bridge = new DevDCardGatewayBridge(delegate);

    @Test
    void mapsDeveloperDResponseIntoDeveloperCPaymentResult() {
        when(delegate.charge(any(), anyString())).thenReturn(new CardPaymentResponse(
                "pay-1", "CAPTURED", true, new BigDecimal("500"), "EGP",
                new BigDecimal("500"), "order-1",
                new CardSummary("411111******1111", "VISA", "CREDIT", "Mona", "12/30"),
                "AUTH", "RRN", "00", "Approved", Map.of(), null, Map.of(),
                "Tuition", null, null));

        CardGatewayPort.Result result = bridge.charge(command(), "idem-1");

        assertThat(result.outcome()).isEqualTo(CardGatewayPort.Outcome.APPROVED);
        assertThat(result.gatewayPaymentId()).isEqualTo("pay-1");
        assertThat(result.cardType()).isEqualTo("CREDIT");
    }

    @Test
    void mapsAProviderDeclineWithoutThrowingOutOfTheSaga() {
        when(delegate.charge(any(), anyString())).thenThrow(new BankMockApiException(
                "CARD_DECLINED", "Insufficient funds",
                Map.of("payment_id", "pay-declined", "response_code", "51")));

        CardGatewayPort.Result result = bridge.charge(command(), "idem-2");

        assertThat(result.outcome()).isEqualTo(CardGatewayPort.Outcome.DECLINED);
        assertThat(result.gatewayPaymentId()).isEqualTo("pay-declined");
        assertThat(result.responseCode()).isEqualTo("51");
    }

    @Test
    void mapsIssuerUnavailabilityToUnknownForReconciliation() {
        when(delegate.charge(any(), anyString())).thenThrow(new BankMockApiException(
                "ISSUER_UNAVAILABLE", "Issuer unavailable", Map.of()));

        assertThat(bridge.charge(command(), "idem-3").outcome())
                .isEqualTo(CardGatewayPort.Outcome.UNKNOWN);
    }

    private CardGatewayPort.ChargeCommand command() {
        return new CardGatewayPort.ChargeCommand(UUID.randomUUID(), "4111111111111111",
                "Mona Samir", 12, 2030, "123", new BigDecimal("500"), "EGP",
                "Tuition", true);
    }
}
