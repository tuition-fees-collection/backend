package com.tuitionnetwork.tuitionnetwork.payment.api;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import com.tuitionnetwork.tuitionnetwork.payment.domain.PaymentMethod;
import com.tuitionnetwork.tuitionnetwork.payment.domain.PaymentStatus;
import com.tuitionnetwork.tuitionnetwork.payment.domain.PaymentType;
import com.tuitionnetwork.tuitionnetwork.payment.port.in.CreatePaymentUseCase;
import com.tuitionnetwork.tuitionnetwork.payment.port.in.CreatePaymentUseCase.PaymentOrderView;
import com.tuitionnetwork.tuitionnetwork.payment.port.in.ExecutePaymentSagaUseCase;
import com.tuitionnetwork.tuitionnetwork.payment.port.in.ExecutePaymentSagaUseCase.PaymentResultView;
import com.tuitionnetwork.tuitionnetwork.payment.port.in.GetPaymentUseCase;
import com.tuitionnetwork.tuitionnetwork.payment.port.in.GetPaymentUseCase.PaymentView;
import com.tuitionnetwork.tuitionnetwork.payment.services.PaymentAccessDeniedException;
import com.tuitionnetwork.tuitionnetwork.payment.services.Paymentvalidationexception;
@WebMvcTest(controllers = PaymentController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(PaymentExceptionHandler.class)
class PaymentControllerTest {

    private static final String GUARDIAN = "33333333-3333-3333-3333-333333333333";
    private static final String STUDENT = "44444444-4444-4444-4444-444444444444";
    private static final String OFFICER = "22222222-2222-2222-2222-222222222222";
    private static final String FEE = "77777777-7777-7777-7777-777777777777";
    private static final UUID PAYMENT_ID = UUID.randomUUID();

    private static final String ORDER_BODY = """
            {
              "studentId": "%s",
              "processedBy": "%s",
              "feeLines": [ { "feeId": "%s", "amountToApply": 500.00 } ],
              "method": "CREDIT_CARD",
              "paymentType": "PARTIAL",
              "currency": "EGP"
            }
            """.formatted(STUDENT, OFFICER, FEE);

    private static final String CARD_BODY = """
            {
              "cardNumber": "4111111111111111",
              "cardHolderName": "Mohamed Hamouda",
              "expiryMonth": 12,
              "expiryYear": 2030,
              "cvv": "123"
            }
            """;

    @Autowired private MockMvc mvc;

    @MockitoBean private CreatePaymentUseCase createPayment;
    @MockitoBean private ExecutePaymentSagaUseCase executePayment;
    @MockitoBean private GetPaymentUseCase getPayment;
    @Nested
    @DisplayName("creating an order")
    class Create {
        @Test
        @DisplayName("returns 201 for a new order")
        void created() throws Exception {
            when(createPayment.create(any())).thenReturn(orderView(false));

            mvc.perform(post("/api/v1/payment-orders")
                            .header("Idempotency-Key", "key-1")
                            .header("X-Guardian-Id", GUARDIAN)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(ORDER_BODY))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.status").value("CREATED"))
                    .andExpect(jsonPath("$.amount").value(500.00));
        }

        @Test
        @DisplayName("returns 200 when the key has already been used")
        void replay() throws Exception {
            when(createPayment.create(any())).thenReturn(orderView(true));

            mvc.perform(post("/api/v1/payment-orders")
                            .header("Idempotency-Key", "key-1")
                            .header("X-Guardian-Id", GUARDIAN)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(ORDER_BODY))
                    .andExpect(status().isOk());
        }
        @Test
        @DisplayName("refuses a request with no Idempotency-Key")
        void idempotencyKeyIsRequired() throws Exception {
            mvc.perform(post("/api/v1/payment-orders")
                            .header("X-Guardian-Id", GUARDIAN)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(ORDER_BODY))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.code").value("MISSING_HEADER"));

            verify(createPayment, never()).create(any());
        }

        @Test
        @DisplayName("refuses a request with no guardian")
        void guardianIsRequired() throws Exception {
            mvc.perform(post("/api/v1/payment-orders")
                            .header("Idempotency-Key", "key-1")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(ORDER_BODY))
                    .andExpect(status().isBadRequest());

            verify(createPayment, never()).create(any());
        }
        @Test
        @DisplayName("rejects an empty fee list before the service is called")
        void emptyFeeLines() throws Exception {
            String body = """
                    {
                      "studentId": "%s",
                      "processedBy": "%s",
                      "feeLines": [],
                      "method": "CREDIT_CARD",
                      "paymentType": "PARTIAL",
                      "currency": "EGP"
                    }
                    """.formatted(STUDENT, OFFICER);

            mvc.perform(post("/api/v1/payment-orders")
                            .header("Idempotency-Key", "key-1")
                            .header("X-Guardian-Id", GUARDIAN)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(body))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));

            verify(createPayment, never()).create(any());
        }
    }

    @Nested
    @DisplayName("charging")
    class Charge {
        @Test
        @DisplayName("a declined card still returns 200")
        void declineIsNotAnHttpError() throws Exception {
            when(executePayment.execute(any())).thenReturn(new PaymentResultView(
                    PAYMENT_ID, PaymentStatus.FAILED, false, null, null,
                    "Your bank declined this payment. Try a different card."));

            mvc.perform(post("/api/v1/payment-orders/" + PAYMENT_ID + "/pay")
                            .header("X-Guardian-Id", GUARDIAN)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(CARD_BODY))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.status").value("FAILED"));
        }

        @Test
        @DisplayName("an unknown outcome reports as pending, not as failure")
        void unknownIsPending() throws Exception {
            when(executePayment.execute(any())).thenReturn(new PaymentResultView(
                    PAYMENT_ID, PaymentStatus.RECON_REQUIRED, false, null, null,
                    "We are confirming this payment with your bank."));

            mvc.perform(post("/api/v1/payment-orders/" + PAYMENT_ID + "/pay")
                            .header("X-Guardian-Id", GUARDIAN)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(CARD_BODY))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.status").value("RECON_REQUIRED"))
                    .andExpect(jsonPath("$.pending").value(true));
        }

        @Test
        @DisplayName("a card charge with no card number is rejected at the edge")
        void cardIsRequired() throws Exception {
            mvc.perform(post("/api/v1/payment-orders/" + PAYMENT_ID + "/pay")
                            .header("X-Guardian-Id", GUARDIAN)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {
                                      "cardHolderName": "Mohamed Hamouda",
                                      "expiryMonth": 12,
                                      "expiryYear": 2030,
                                      "cvv": "123"
                                    }
                                    """))
                    .andExpect(status().isBadRequest());

            verify(executePayment, never()).execute(any());
        }
        @Test
        @DisplayName("a cash payment needs no body")
        void cashTakesNoBody() throws Exception {
            when(executePayment.execute(any())).thenReturn(new PaymentResultView(
                    PAYMENT_ID, PaymentStatus.SUCCESSFUL, false, null, "RCP-2026-TESTTEST",
                    "Payment successful"));

            mvc.perform(post("/api/v1/payment-orders/" + PAYMENT_ID + "/pay-cash")
                            .header("X-Guardian-Id", GUARDIAN))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.status").value("SUCCESSFUL"));
        }
    }

    @Nested
    @DisplayName("errors")
    class Errors {
        @Test
        @DisplayName("another guardian's payment is 404, not 403")
        void accessDeniedLooksLikeNotFound() throws Exception {
            when(getPayment.getById(any(), any()))
                    .thenThrow(new PaymentAccessDeniedException(UUID.fromString(GUARDIAN), PAYMENT_ID));

            mvc.perform(get("/api/v1/payment-orders/" + PAYMENT_ID)
                            .header("X-Guardian-Id", GUARDIAN))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.message").value("Payment not found"));
        }

        @Test
        @DisplayName("a business rejection is 422 and carries its code")
        void validationIs422() throws Exception {
            when(createPayment.create(any())).thenThrow(
                    new Paymentvalidationexception("FEE_NOT_PAYABLE", "This fee is no longer outstanding"));

            mvc.perform(post("/api/v1/payment-orders")
                            .header("Idempotency-Key", "key-1")
                            .header("X-Guardian-Id", GUARDIAN)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(ORDER_BODY))
                    .andExpect(status().isUnprocessableContent())
                    .andExpect(jsonPath("$.code").value("FEE_NOT_PAYABLE"));
        }
    }

    @Nested
    @DisplayName("what the response must not contain")
    class Leakage {
        @Test
        @DisplayName("support and reconciliation fields never reach the caller")
        void noInternalFields() throws Exception {
            when(getPayment.getById(any(), any())).thenReturn(paymentView());

            mvc.perform(get("/api/v1/payment-orders/" + PAYMENT_ID)
                            .header("X-Guardian-Id", GUARDIAN))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.authCode").doesNotExist())
                    .andExpect(jsonPath("$.rrn").doesNotExist())
                    .andExpect(jsonPath("$.responseCode").doesNotExist())
                    .andExpect(jsonPath("$.processedBy").doesNotExist())
                    .andExpect(jsonPath("$.maskedCardNumber").value("411111******1111"));
        }
    }
    private static PaymentOrderView orderView(boolean replay) {
        Instant now = Instant.now();
        return new PaymentOrderView(PAYMENT_ID, PaymentStatus.CREATED, new BigDecimal("500.00"),
                "EGP", PaymentMethod.CREDIT_CARD, PaymentType.PARTIAL, now,
                now.plusSeconds(900), replay);
    }
    private static PaymentView paymentView() {
        return new PaymentView(PAYMENT_ID, PaymentStatus.SUCCESSFUL, new BigDecimal("500.00"),
                "EGP", PaymentMethod.CREDIT_CARD, PaymentType.PARTIAL,
                "411111******1111", "VISA", Instant.now(), Instant.now(),
                "RCP-2026-TESTTEST", List.of());
    }
}