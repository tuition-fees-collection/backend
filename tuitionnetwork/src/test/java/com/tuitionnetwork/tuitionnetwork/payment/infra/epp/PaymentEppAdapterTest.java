package com.tuitionnetwork.tuitionnetwork.payment.infra.epp;

import com.tuitionnetwork.tuitionnetwork.identity.service.IdentityAccessService;
import com.tuitionnetwork.tuitionnetwork.payment.domain.Payment;
import com.tuitionnetwork.tuitionnetwork.payment.domain.PaymentType;
import com.tuitionnetwork.tuitionnetwork.payment.port.portout.PaymentRepository;
import com.tuitionnetwork.tuitionnetwork.payment.services.PaymentAccessDeniedException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.access.AccessDeniedException;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class PaymentEppAdapterTest {

    private final PaymentRepository repository = mock(PaymentRepository.class);
    private final IdentityAccessService identity = mock(IdentityAccessService.class);
    private final PaymentEppAdapter adapter = new PaymentEppAdapter(repository, identity);
    private Payment payment;
    private UUID paymentId;
    private UUID guardianId;

    @BeforeEach
    void setUp() {
        payment = mock(Payment.class);
        paymentId = UUID.randomUUID();
        guardianId = UUID.randomUUID();
        when(payment.getId()).thenReturn(paymentId);
        when(payment.getGuardianId()).thenReturn(guardianId);
        when(repository.findById(paymentId)).thenReturn(Optional.of(payment));
    }

    @Test
    void returnsTrustedDetailsForSuccessfulEppCreditPayment() {
        when(payment.getPaymentType()).thenReturn(PaymentType.EPP);
        when(payment.isEppEligible()).thenReturn(true);
        when(payment.getGatewayPaymentId()).thenReturn("pay-trusted");
        when(payment.getAmount()).thenReturn(new BigDecimal("6000.00"));
        when(payment.getCurrency()).thenReturn("EGP");

        var result = adapter.requireEligiblePayment(paymentId);

        assertThat(result.gatewayPaymentId()).isEqualTo("pay-trusted");
        assertThat(result.amount()).isEqualByComparingTo("6000.00");
        assertThat(result.currency()).isEqualTo("EGP");
        verify(identity).authorizePaymentAccess(guardianId);
    }

    @Test
    void rejectsPaymentThatWasNotCreatedForEpp() {
        when(payment.getPaymentType()).thenReturn(PaymentType.FULL);
        when(payment.isEppEligible()).thenReturn(true);

        assertThatThrownBy(() -> adapter.requireEligiblePayment(paymentId))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("successful EPP credit-card payment");
    }

    @Test
    void hidesPaymentWhenCallerIsUnauthorized() {
        doThrow(new AccessDeniedException("denied"))
                .when(identity).authorizePaymentAccess(guardianId);

        assertThatThrownBy(() -> adapter.requireEligiblePayment(paymentId))
                .isInstanceOf(PaymentAccessDeniedException.class)
                .hasMessage("Payment not found");
    }
}
