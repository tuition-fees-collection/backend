package com.tuitionnetwork.tuitionnetwork.payment.api;
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import com.tuitionnetwork.tuitionnetwork.payment.domain.PaymentMethod;
import com.tuitionnetwork.tuitionnetwork.payment.domain.PaymentType;
import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
public record PaymentRequest(
        @NotNull(message = "studentId is required")
        UUID studentId,
        @NotEmpty(message = "At least one fee line is required")
        @Valid
        List<FeeLineRequest> feeLines,
        @NotNull(message = "method is required")
        PaymentMethod method,
        @NotNull(message = "paymentType is required")
        PaymentType paymentType,
        String currency
) {
    public record FeeLineRequest(
            @NotNull(message = "feeId is required")
            UUID feeId,
            @NotNull(message = "amountToApply is required")
            @DecimalMin(value = "0.01", message = "amountToApply must be greater than zero")
            BigDecimal amountToApply
    ) {
    }
}