package com.tuitionnetwork.tuitionnetwork.payment.port.in;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import com.tuitionnetwork.tuitionnetwork.payment.domain.PaymentMethod;
import com.tuitionnetwork.tuitionnetwork.payment.domain.PaymentStatus;
import com.tuitionnetwork.tuitionnetwork.payment.domain.PaymentType;
class PaymentCommandsTest {
    private static final UUID STUDENT = UUID.randomUUID();

    @Nested
    @DisplayName("CreatePaymentCommand")
    class Create {

        @Test
        @DisplayName("the amount is derived from the fee lines, never supplied by the caller")
        void amountIsDerived() {
            var command = command(List.of(
                    new CreatePaymentUseCase.FeeLine(UUID.randomUUID(), new BigDecimal("8000.00")),
                    new CreatePaymentUseCase.FeeLine(UUID.randomUUID(), new BigDecimal("2000.00"))));
            assertEquals(0, new BigDecimal("10000.00").compareTo(command.totalAmount()));
        }

        @Test
        @DisplayName("a payment must settle at least one fee line")
        void rejectsEmptyFeeLines() {
            assertThrows(IllegalArgumentException.class, () -> command(List.of()));
            assertThrows(IllegalArgumentException.class, () -> command(null));
        }

        @Test
        @DisplayName("the fee lines are copied, so the command cannot change after construction")
        void feeLinesAreImmutable() {
            List<CreatePaymentUseCase.FeeLine> lines = new ArrayList<>();
            lines.add(new CreatePaymentUseCase.FeeLine(UUID.randomUUID(), new BigDecimal("100.00")));

            var command = command(lines);
            lines.add(new CreatePaymentUseCase.FeeLine(UUID.randomUUID(), new BigDecimal("999.00")));

            assertEquals(1, command.feeLines().size(), "the command must not see later changes");
            assertEquals(0, new BigDecimal("100.00").compareTo(command.totalAmount()));
            assertThrows(UnsupportedOperationException.class, () -> command.feeLines().add(null));
        }

        @Test
        @DisplayName("a fee line cannot apply zero or a negative amount")
        void rejectsNonPositiveFeeLine() {
            UUID fee = UUID.randomUUID();
            assertThrows(IllegalArgumentException.class,
                    () -> new CreatePaymentUseCase.FeeLine(fee, BigDecimal.ZERO));
            assertThrows(IllegalArgumentException.class,
                    () -> new CreatePaymentUseCase.FeeLine(fee, new BigDecimal("-100.00")));
            assertThrows(IllegalArgumentException.class,
                    () -> new CreatePaymentUseCase.FeeLine(fee, null));
        }

        private CreatePaymentUseCase.CreatePaymentCommand command(List<CreatePaymentUseCase.FeeLine> lines) {
            return new CreatePaymentUseCase.CreatePaymentCommand("idem-1",
                    STUDENT, lines, PaymentMethod.CREDIT_CARD, PaymentType.PARTIAL, "EGP");
        }
    }

    @Nested
    @DisplayName("card data never reaches a log")
    class Redaction {

        @Test
        @DisplayName("ExecutePaymentCommand.toString hides the PAN and the CVV")
        void executeCommandIsRedacted() {
            var command = new ExecutePaymentSagaUseCase.ExecutePaymentCommand(
                    UUID.randomUUID(), "4111111111111111", "MOHAMED HAMOUDA", 12, 2030, "123");

            String printed = command.toString();
            assertFalse(printed.contains("4111111111111111"), "toString must not print the card number");
            assertFalse(printed.contains("111111"), "toString must not print any part of the PAN");
            assertFalse(printed.contains("123"), "toString must not print the CVV");
            assertTrue(printed.contains("REDACTED"));
        }

        @Test
        @DisplayName("Confirm3dsCommand.toString hides the OTP")
        void confirm3dsIsRedacted() {
            var command = new ExecutePaymentSagaUseCase.Confirm3dsCommand(
                    UUID.randomUUID(), "123456");

            String printed = command.toString();
            assertFalse(printed.contains("123456"), "an OTP in a log is a replayable credential");
            assertTrue(printed.contains("REDACTED"));
        }
    }

    @Nested
    @DisplayName("PaymentResultView")
    class ResultView {

        @ParameterizedTest
        @EnumSource(value = PaymentStatus.class, names = {"CREATED", "PENDING", "RECON_REQUIRED"})
        @DisplayName("an unresolved outcome reports as pending, never as success or failure")
        void unresolvedIsPending(PaymentStatus status) {
            assertTrue(view(status).isPending(),
                    status + " must be reported as pending - BO-US-08 AC4");
        }

        @ParameterizedTest
        @EnumSource(value = PaymentStatus.class, names = {"SUCCESSFUL", "FAILED"})
        @DisplayName("a settled outcome is not pending")
        void terminalIsNotPending(PaymentStatus status) {
            assertFalse(view(status).isPending());
        }

        private ExecutePaymentSagaUseCase.PaymentResultView view(PaymentStatus status) {
            return new ExecutePaymentSagaUseCase.PaymentResultView(
                    UUID.randomUUID(), status, false, "411111******1111", null, "message");
        }
    }

    @Nested
    @DisplayName("GetPaymentUseCase views")
    class Views {

        @Test
        @DisplayName("a null allocation list becomes empty rather than an NPE in the controller")
        void nullAllocationsBecomeEmpty() {
            var view = new GetPaymentUseCase.PaymentView(UUID.randomUUID(), PaymentStatus.SUCCESSFUL,
                    new BigDecimal("100.00"), "EGP", PaymentMethod.CASH, PaymentType.FULL,
                    null, null, null, null, null, null);
            assertEquals(List.of(), view.allocations());
        }
    }
}
