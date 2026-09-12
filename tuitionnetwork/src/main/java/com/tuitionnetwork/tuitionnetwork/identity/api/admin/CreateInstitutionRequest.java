package com.tuitionnetwork.tuitionnetwork.identity.api.admin;

import jakarta.validation.constraints.NotBlank;

public record CreateInstitutionRequest(
        @NotBlank String name,
        @NotBlank String type,
        @NotBlank String status
) {
}
