package com.tuitionnetwork.tuitionnetwork.identity.api.admin;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record CreateInstitutionRequest(
        @NotBlank(message = "Institution name is required")
        @Size(max = 255, message = "Institution name must be 255 characters or fewer")
        String name,

        @NotBlank(message = "Institution type is required")
        @Pattern(
                regexp = "SCHOOL|UNIVERSITY|INSTITUTE",
                message = "Type must be SCHOOL, UNIVERSITY, or INSTITUTE"
        )
        String type,

        @NotBlank(message = "Status is required")
        @Pattern(
                regexp = "ACTIVE|INACTIVE",
                message = "Status must be ACTIVE or INACTIVE"
        )
        String status
) {
}
