package com.tuitionnetwork.tuitionnetwork.identity.api.admin;

import java.util.UUID;

public record InstitutionUserResponse(
        UUID userId,
        UUID institutionId,
        String fullName,
        String role,
        UUID accountId,
        String username,
        String accountStatus
) {
}
