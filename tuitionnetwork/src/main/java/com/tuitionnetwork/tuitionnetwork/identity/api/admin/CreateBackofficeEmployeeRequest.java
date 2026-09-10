package com.tuitionnetwork.tuitionnetwork.identity.api.admin;

import com.tuitionnetwork.tuitionnetwork.identity.domain.RoleCode;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record CreateBackofficeEmployeeRequest(

        @NotBlank
        String fullName,

        @NotBlank
        String branch,

        @NotNull
        RoleCode role,

        @NotBlank
        String username,

        @NotBlank
        String password
) {
}
