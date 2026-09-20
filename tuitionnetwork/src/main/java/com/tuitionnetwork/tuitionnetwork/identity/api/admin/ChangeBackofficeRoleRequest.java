package com.tuitionnetwork.tuitionnetwork.identity.api.admin;

import com.tuitionnetwork.tuitionnetwork.identity.domain.RoleCode;
import jakarta.validation.constraints.NotNull;

public record ChangeBackofficeRoleRequest(

        @NotNull(message = "Role is required")
        RoleCode role
) {
}
