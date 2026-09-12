package com.tuitionnetwork.tuitionnetwork.payment.services.strategy;
import com.tuitionnetwork.tuitionnetwork.payment.domain.Payment;
import com.tuitionnetwork.tuitionnetwork.payment.port.in.ExecutePaymentSagaUseCase.ExecutePaymentCommand;
import com.tuitionnetwork.tuitionnetwork.payment.port.portout.CardGatewayPort.ChargeCommand;
final class CardCharges {

    private CardCharges() {
    }

    static ChargeCommand from(Payment payment, ExecutePaymentCommand command) {
        return new ChargeCommand(
                payment.getId(),
                command.cardNumber(),
                command.cardHolderName(),
                command.expiryMonth(),
                command.expiryYear(),
                command.cvv(),
                payment.getAmount(),
                payment.getCurrency(),
                "Tuition payment",
                true);
    }
    static String keyFor(Payment payment) {
        return "pay-" + payment.getId();
    }
}