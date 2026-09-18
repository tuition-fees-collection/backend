package com.tuitionnetwork.tuitionnetwork.payment.saga;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import com.tuitionnetwork.tuitionnetwork.payment.domain.Payment;
import com.tuitionnetwork.tuitionnetwork.payment.domain.PaymentMethod;
import com.tuitionnetwork.tuitionnetwork.payment.domain.PaymentSagaStep;
import com.tuitionnetwork.tuitionnetwork.payment.domain.PaymentSagaStepType;
import com.tuitionnetwork.tuitionnetwork.payment.domain.PaymentStatus;
import com.tuitionnetwork.tuitionnetwork.payment.port.in.ExecutePaymentSagaUseCase;
import com.tuitionnetwork.tuitionnetwork.payment.port.portout.Backofficegatewayport;
import com.tuitionnetwork.tuitionnetwork.payment.port.portout.CardGatewayPort;
import com.tuitionnetwork.tuitionnetwork.payment.port.portout.PaymentRepository;
import com.tuitionnetwork.tuitionnetwork.payment.port.portout.ReceiptPort;
import com.tuitionnetwork.tuitionnetwork.identity.service.IdentityAccessService;
import com.tuitionnetwork.tuitionnetwork.payment.services.PaymentAccessDeniedException;
import com.tuitionnetwork.tuitionnetwork.payment.services.strategy.PaymentMethodHandler;
import org.springframework.security.access.AccessDeniedException;
@Service
public class PaymentSagaOrchestrator implements ExecutePaymentSagaUseCase {
    private static final Logger log = LoggerFactory.getLogger(PaymentSagaOrchestrator.class);
    private final PaymentRepository payments;
    private final PaymentSettlement settlement;
    private final PaymentSaga saga;
    private final ReceiptPort receipts;
    private final CardGatewayPort gateway;
    private final Backofficegatewayport backoffice;
    private final IdentityAccessService identity;
    private final Map<PaymentMethod, PaymentMethodHandler> handlers = new EnumMap<>(PaymentMethod.class);
    public PaymentSagaOrchestrator(PaymentRepository payments,
                                   PaymentSettlement settlement,
                                   PaymentSaga saga,
                                   ReceiptPort receipts,
                                   CardGatewayPort gateway,
                                   Backofficegatewayport backoffice,
                                   IdentityAccessService identity,
                                   List<PaymentMethodHandler> methodHandlers) {
        this.payments = payments;
        this.settlement = settlement;
        this.saga = saga;
        this.receipts = receipts;
        this.gateway = gateway;
        this.backoffice = backoffice;
        this.identity = identity;
        methodHandlers.forEach(handler -> this.handlers.put(handler.handles(), handler));
                                   }
    @Override
    public PaymentResultView execute(ExecutePaymentCommand command) {
        Payment payment = load(command.paymentId());
        if (payment.getMethod() == PaymentMethod.CASH) {
            identity.requireCashCollection();
        }
        payment = settlement.markPending(payment);
        if (payment.getMethod() == PaymentMethod.CASH
                && (command.hasCard() || command.hasSource())) {
            throw new IllegalStateException("A cash payment is taken through /pay-cash");
        }
        if (payment.getMethod().usesHeldSource() && !command.hasSource()) {
            throw new IllegalStateException("This payment needs a source - use /pay-source");
        }
        if (payment.getMethod().requiresGateway()
                && !payment.getMethod().usesHeldSource()
                && !command.hasCard()
            && !command.hasSource()) {
            throw new IllegalStateException("A card payment needs card details - use /pay");
        }
        if (command.hasSource() && !sourceBelongsToCustomer(command)) {
            throw new IllegalStateException(
                    "That payment source does not belong to this customer");
        }

        PaymentMethodHandler handler = handlers.get(payment.getMethod());
        if (handler == null) {
            payment = settlement.settleFailure(payment,
                    "Unsupported payment method: " + payment.getMethod(), null);
            return view(payment, false, "This payment method is not available");
        }
        PaymentSagaStep charge = saga.begin(payment.getId(), PaymentSagaStepType.CHARGE);
        if (payment.getMethod() == PaymentMethod.CASH) {
            saga.skip(payment.getId(), PaymentSagaStepType.CONFIRM_3DS, "Cash needs no 3-D Secure");
        }
        CardGatewayPort.Result result = handler.charge(payment, command);
        return apply(payment, charge, result);
    }
    @Override
    public PaymentResultView confirm3ds(Confirm3dsCommand command) {
        Payment payment = load(command.paymentId());

        if (payment.getGatewayPaymentId() == null) {
            throw new IllegalStateException("This payment has no provider reference to confirm");
        }

        PaymentSagaStep confirm = saga.begin(payment.getId(), PaymentSagaStepType.CONFIRM_3DS);
        CardGatewayPort.Result result = gateway.confirm3ds(payment.getGatewayPaymentId(), command.otp());

        return apply(payment, confirm, result);
    }
    private PaymentResultView apply(Payment payment, PaymentSagaStep step, CardGatewayPort.Result result) {
        switch (result.outcome()) {
            case APPROVED -> {
                saga.succeed(step);
                return view(settlement.settleSuccess(payment, result), false, "Payment successful", result);
            }

            case REQUIRES_3DS -> {
                Payment awaiting = settlement.attachGatewayId(payment, result.gatewayPaymentId());
                log.info("Payment {} is awaiting 3-D Secure", awaiting.getId());
                return new PaymentResultView(awaiting.getId(), awaiting.getStatus(),
                        true, result.maskedCardNumber(), null,
                        "Enter the code sent to your phone");
            }

            case DECLINED, REJECTED -> {
                saga.fail(step, result.errorCode() + ": " + result.responseMessage());
                String message = safeMessage(result);
                return view(settlement.settleFailure(payment, message, result), false, message, result);
            }

            case UNKNOWN, NOT_FOUND -> {
                saga.leaveUnresolved(step, result.responseMessage());
                Payment flagged = settlement.flagForReconciliation(payment, result);
                return view(flagged, false,
                        "We are confirming this payment with your bank. Do not try again yet.");
            }
        }

        throw new IllegalStateException("Unhandled gateway outcome: " + result.outcome());
    }
  private boolean sourceBelongsToCustomer(ExecutePaymentCommand command) {
    return backoffice.lookupCustomer(command.nationalId())
            .filter(profile -> profile.isActive())
            .map(profile -> profile.holds(command.sourceId()))
            .orElse(false);
}

    private Payment load(UUID paymentId) {
        Payment payment = payments.findById(paymentId)
                .orElseThrow(() -> new PaymentAccessDeniedException(null, paymentId));
        try {
            identity.authorizePaymentAccess(payment.getGuardianId());
        } catch (AccessDeniedException e) {
            throw new PaymentAccessDeniedException(null, paymentId);
        }
        return payment;
    }
    private String safeMessage(CardGatewayPort.Result result) {
        if ("CARD_DECLINED".equals(result.errorCode())) {
            return "Your bank declined this payment. Try a different card.";
        }
        if ("CARD_EXPIRED".equals(result.errorCode())) {
            return "That card has expired.";
        }
        return "This payment could not be completed.";
    }

    private PaymentResultView view(Payment payment, boolean requires3ds, String message) {
        return view(payment, requires3ds, message, null);
    }
    private PaymentResultView view(Payment payment, boolean requires3ds, String message,
                                   CardGatewayPort.Result result) {
        String receiptReference = payment.getStatus() == PaymentStatus.SUCCESSFUL
                ? receipts.findByPaymentId(payment.getId())
                        .map(receipt -> receipt.referenceNumber())
                        .orElse(null)
                : null;

        return new PaymentResultView(payment.getId(), payment.getStatus(), requires3ds,
                payment.getMaskedCardNumber(), receiptReference, message,
                providerDetail(result));
    }
    private static ExecutePaymentSagaUseCase.ProviderDetail providerDetail(
            CardGatewayPort.Result result) {

        if (result == null) {
            return null;
        }
        ExecutePaymentSagaUseCase.ProviderDetail.Source source =
                new ExecutePaymentSagaUseCase.ProviderDetail.Source(
                        result.cardScheme() == null ? "ACCOUNT" : "CARD",
                        result.cardType(),
                        result.maskedCardNumber(),
                        result.cardScheme());

        return new ExecutePaymentSagaUseCase.ProviderDetail(
                result.gatewayPaymentId(),
                result.gatewayStatus(),
                result.outcome() == CardGatewayPort.Outcome.APPROVED,
                result.amount(),
                result.capturedAmount(),
                result.reference(),
                result.customerId(),
                result.sourceId(),
                source,
                result.authCode(),
                result.rrn(),
                result.responseCode(),
                result.responseMessage());
    }
}