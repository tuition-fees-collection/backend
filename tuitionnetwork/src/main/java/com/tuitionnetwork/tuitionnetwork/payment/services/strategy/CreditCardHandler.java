package com.tuitionnetwork.tuitionnetwork.payment.services.strategy;

import org.springframework.stereotype.Component;
import com.tuitionnetwork.tuitionnetwork.payment.domain.Payment;
import com.tuitionnetwork.tuitionnetwork.payment.domain.PaymentMethod;
import com.tuitionnetwork.tuitionnetwork.payment.port.in.ExecutePaymentSagaUseCase.ExecutePaymentCommand;
import com.tuitionnetwork.tuitionnetwork.payment.port.portout.Backofficegatewayport;
import com.tuitionnetwork.tuitionnetwork.payment.port.portout.CardGatewayPort;

@Component
public class CreditCardHandler implements PaymentMethodHandler {

    private final CardGatewayPort gateway;
    private final Backofficegatewayport backoffice;

    public CreditCardHandler(CardGatewayPort gateway, Backofficegatewayport backoffice) {
        this.gateway = gateway;
        this.backoffice = backoffice;
    }

    @Override
    public PaymentMethod handles() {
        return PaymentMethod.CREDIT_CARD;
    }

    @Override
    public CardGatewayPort.Result charge(Payment payment, ExecutePaymentCommand command) {
        if (command.hasSource()) {
            return backoffice.chargeSource(
                    new Backofficegatewayport.SourceChargeCommand(
                            payment.getId(),
                            command.sourceId(),
                            payment.getAmount(),
                            payment.getCurrency(),
                            payment.getId().toString(),
                            "Tuition payment",
                            true),
                    "pay-" + payment.getId());
        }
        return gateway.charge(CardCharges.from(payment, command), CardCharges.keyFor(payment));
    }
}