package com.tuitionnetwork.tuitionnetwork.payment.api;
import jakarta.validation.constraints.NotBlank;
public record SourceChargeRequest(

        @NotBlank(message = "sourceId is required")
        String sourceId,
        @NotBlank(message = "nationalId is required")
        String nationalId
) {
}
