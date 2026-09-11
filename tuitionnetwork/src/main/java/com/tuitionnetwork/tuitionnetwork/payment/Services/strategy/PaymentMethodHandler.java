package com.tuitionnetwork.tuitionnetwork.payment.services.strategy;
import com.tuitionnetwork.tuitionnetwork.payment.domain.Payment;
import com.tuitionnetwork.tuitionnetwork.payment.domain.PaymentMethod;
import com.tuitionnetwork.tuitionnetwork.payment.port.in.ExecutePaymentSagaUseCase.ExecutePaymentCommand;
import com.tuitionnetwork.tuitionnetwork.payment.port.portout.CardGatewayPort;
public interface PaymentMethodHandler {
    PaymentMethod handles();
    CardGatewayPort.Result charge(Payment payment, ExecutePaymentCommand command);
}
