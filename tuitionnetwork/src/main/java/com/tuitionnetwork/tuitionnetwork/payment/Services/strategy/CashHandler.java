package com.tuitionnetwork.tuitionnetwork.payment.services.strategy;
import java.math.BigDecimal;
import org.springframework.stereotype.Component;
import com.tuitionnetwork.tuitionnetwork.payment.domain.Payment;
import com.tuitionnetwork.tuitionnetwork.payment.domain.PaymentMethod;
import com.tuitionnetwork.tuitionnetwork.payment.port.in.ExecutePaymentSagaUseCase.ExecutePaymentCommand;
import com.tuitionnetwork.tuitionnetwork.payment.port.portout.CardGatewayPort;
@Component
public class CashHandler implements PaymentMethodHandler {
    @Override
    public PaymentMethod handles() {
        return PaymentMethod.CASH;
    }
    @Override
    public CardGatewayPort.Result charge(Payment payment, ExecutePaymentCommand command) {
        BigDecimal amount = payment.getAmount();

        return new CardGatewayPort.Result(
                CardGatewayPort.Outcome.APPROVED,
                null,              
                "CASH_RECEIVED",   
                amount,            
                null, null,        
                "00",
                "Cash received at branch",
                null, null, null,  
                null);
    }
}
