package com.tuitionnetwork.tuitionnetwork.payment.api;
import java.util.List;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import com.tuitionnetwork.tuitionnetwork.payment.port.in.CreatePaymentUseCase;
import com.tuitionnetwork.tuitionnetwork.payment.port.in.CreatePaymentUseCase.CreatePaymentCommand;
import com.tuitionnetwork.tuitionnetwork.payment.port.in.CreatePaymentUseCase.FeeLine;
import com.tuitionnetwork.tuitionnetwork.payment.port.in.CreatePaymentUseCase.PaymentOrderView;
import com.tuitionnetwork.tuitionnetwork.payment.port.in.ExecutePaymentSagaUseCase;
import com.tuitionnetwork.tuitionnetwork.payment.port.in.ExecutePaymentSagaUseCase.Confirm3dsCommand;
import com.tuitionnetwork.tuitionnetwork.payment.port.in.ExecutePaymentSagaUseCase.ExecutePaymentCommand;
import com.tuitionnetwork.tuitionnetwork.payment.port.in.ExecutePaymentSagaUseCase.PaymentResultView;
import com.tuitionnetwork.tuitionnetwork.payment.port.in.GetPaymentUseCase;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@RestController
@RequestMapping("/api/v1/payment-orders")
//Tells Swagger that every route in this controller requires a Bearer JWT token.
@SecurityRequirement(name = "bearerAuth")
public class PaymentController {

    private static final Logger log = LoggerFactory.getLogger(PaymentController.class);
//Port for creating new payment orders
    private final CreatePaymentUseCase createPayment;
    //Port for processing payments
    private final ExecutePaymentSagaUseCase executePayment;
    //Port for fetching payment details, receipts, and transaction history.
    private final GetPaymentUseCase getPayment;

    public PaymentController(CreatePaymentUseCase createPayment,
                             ExecutePaymentSagaUseCase executePayment,
                             GetPaymentUseCase getPayment) {
        this.createPayment = createPayment;
        this.executePayment = executePayment;
        this.getPayment = getPayment;
    }

    @PostMapping
    public ResponseEntity<PaymentResponse> create(
            @RequestHeader("Idempotency-Key") String idempotencyKey,
            @Valid @RequestBody PaymentRequest request) {

        PaymentOrderView view = createPayment.create(new CreatePaymentCommand(
                idempotencyKey,
                request.studentId(),
                request.feeLines().stream()
                        .map(line -> new FeeLine(line.feeId(), line.amountToApply()))
                        .toList(),
                request.method(),
                request.paymentType(),
                request.currency()));

        log.info("Payment order {} ({})",
                view.paymentId(), view.idempotentReplay() ? "replay" : "new");

        return ResponseEntity
                .status(view.idempotentReplay() ? HttpStatus.OK : HttpStatus.CREATED)
                .body(PaymentResponse.from(view));
    }

    @PostMapping("/{paymentId}/pay")
    public PaymentResultView pay(
            @PathVariable UUID paymentId,
            @Valid @RequestBody ChargeRequest request) {
        log.info("Charging payment {}", paymentId);
        return executePayment.execute(new ExecutePaymentCommand(
                paymentId,
                request.cardNumber(),
                request.cardHolderName(),
                request.expiryMonth(),
                request.expiryYear(),
                request.cvv()));
    }

    @PostMapping("/{paymentId}/pay-cash")
    public PaymentResultView payCash(@PathVariable UUID paymentId) {
        log.info("Taking cash payment {}", paymentId);
        return executePayment.execute(new ExecutePaymentCommand(
                paymentId, null, null, 0, 0, null));
    }

    @PostMapping("/{paymentId}/3ds")
    public PaymentResultView confirm3ds(
            @PathVariable UUID paymentId,
            @Valid @RequestBody OtpRequest request) {
        return executePayment.confirm3ds(new Confirm3dsCommand(paymentId, request.otp()));
    }

    @GetMapping("/{paymentId}")
    public PaymentResponse getById(@PathVariable UUID paymentId) {
        return PaymentResponse.from(getPayment.getById(paymentId));
    }

    @GetMapping("/{paymentId}/receipt")
    public ReceiptResponse receipt(@PathVariable UUID paymentId) {
        var payment = getPayment.getById(paymentId);
        if (payment.receiptReference() == null) {
            throw new IllegalStateException("This payment has no receipt yet");
        }
        return ReceiptResponse.from(payment);
    }

    @GetMapping
    public List<PaymentResponse> history() {
        return getPayment.history().stream()
                .map(PaymentResponse::from)
                .toList();
    }

    public record ChargeRequest(
            @NotBlank(message = "cardNumber is required") String cardNumber,
            @NotBlank(message = "cardHolderName is required")
            @Size(min = 2, message = "cardHolderName must be at least 2 characters")
            String cardHolderName,
            @Min(1) @Max(12) int expiryMonth,
            @Min(2024) int expiryYear,
            @NotBlank(message = "cvv is required") String cvv) {

        @Override
        public String toString() {
            return "ChargeRequest[card=***REDACTED***]";
        }
    }

    public record OtpRequest(@NotBlank(message = "otp is required") String otp) {
        @Override
        public String toString() {
            return "OtpRequest[otp=***REDACTED***]";
        }
    }
}
