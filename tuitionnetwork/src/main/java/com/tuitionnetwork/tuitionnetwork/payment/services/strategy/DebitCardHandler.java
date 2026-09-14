package com.tuitionnetwork.tuitionnetwork.payment.services.strategy;
import org.springframework.stereotype.Component;
import com.tuitionnetwork.tuitionnetwork.payment.domain.Payment;
import com.tuitionnetwork.tuitionnetwork.payment.domain.PaymentMethod;
import com.tuitionnetwork.tuitionnetwork.payment.port.in.ExecutePaymentSagaUseCase.ExecutePaymentCommand;
import com.tuitionnetwork.tuitionnetwork.payment.port.portout.CardGatewayPort;
@Component
public class DebitCardHandler implements PaymentMethodHandler {
    private final CardGatewayPort gateway;
    public DebitCardHandler(CardGatewayPort gateway) {
        this.gateway = gateway;
    }
    @Override
    public PaymentMethod handles() {
        return PaymentMethod.DEBIT_CARD;
    }
    @Override
    public CardGatewayPort.Result charge(Payment payment, ExecutePaymentCommand command) {
        return gateway.charge(CardCharges.from(payment, command), CardCharges.keyFor(payment));
    }
}