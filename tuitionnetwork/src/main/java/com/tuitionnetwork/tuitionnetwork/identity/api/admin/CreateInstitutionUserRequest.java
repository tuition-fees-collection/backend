package com.tuitionnetwork.tuitionnetwork.identity.api.admin;

import jakarta.validation.constraints.NotBlank;

public record CreateInstitutionUserRequest(
        @NotBlank String fullName,
        @NotBlank String username,
        @NotBlank String password
) {
}
