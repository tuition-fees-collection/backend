package com.tuitionnetwork.tuitionnetwork.payment.infra.epp;

import com.tuitionnetwork.tuitionnetwork.epp.service.EppPaymentPort;
import com.tuitionnetwork.tuitionnetwork.identity.service.IdentityAccessService;
import com.tuitionnetwork.tuitionnetwork.payment.domain.Payment;
import com.tuitionnetwork.tuitionnetwork.payment.domain.PaymentType;
import com.tuitionnetwork.tuitionnetwork.payment.port.portout.PaymentRepository;
import com.tuitionnetwork.tuitionnetwork.payment.services.PaymentAccessDeniedException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Component
public class PaymentEppAdapter implements EppPaymentPort {

    private final PaymentRepository paymentRepository;
    private final IdentityAccessService identityAccessService;

    public PaymentEppAdapter(PaymentRepository paymentRepository,
                             IdentityAccessService identityAccessService) {
        this.paymentRepository = paymentRepository;
        this.identityAccessService = identityAccessService;
    }

    @Override
    @Transactional(readOnly = true)
    public EligiblePayment requireEligiblePayment(UUID paymentId) {
        Payment payment = paymentRepository.findById(paymentId)
                .orElseThrow(() -> new PaymentAccessDeniedException(null, paymentId));
        try {
            identityAccessService.authorizePaymentAccess(payment.getGuardianId());
        } catch (AccessDeniedException exception) {
            throw new PaymentAccessDeniedException(null, paymentId);
        }

        if (payment.getPaymentType() != PaymentType.EPP || !payment.isEppEligible()) {
            throw new IllegalStateException(
                    "Only a successful EPP credit-card payment can be converted to installments");
        }
        if (payment.getGatewayPaymentId() == null || payment.getGatewayPaymentId().isBlank()) {
            throw new IllegalStateException("The payment has no gateway reference");
        }

        return new EligiblePayment(payment.getId(), payment.getGatewayPaymentId(),
                payment.getAmount(), payment.getCurrency());
    }
}
