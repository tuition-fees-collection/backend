package com.tuitionnetwork.tuitionnetwork.identity.api.admin;

import com.tuitionnetwork.tuitionnetwork.identity.domain.RoleCode;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record CreateBackofficeEmployeeRequest(

        @NotBlank(message = "Full name is required")
        @Size(max = 255, message = "Full name must be 255 characters or fewer")
        String fullName,

        @NotBlank(message = "Branch is required")
        @Size(max = 100, message = "Branch must be 100 characters or fewer")
        String branch,

        @NotNull(message = "Role is required")
        RoleCode role,

        @NotBlank(message = "Username is required")
        @Size(min = 3, max = 150, message = "Username must be 3 to 150 characters")
        @Pattern(
                regexp = "^[a-zA-Z0-9._-]+$",
                message = "Username may contain letters, numbers, dots, underscores, and hyphens only"
        )
        String username,

        @NotBlank(message = "Password is required")
        @Size(min = 8, max = 100, message = "Password must be at least 8 characters")
        String password
) {
}
