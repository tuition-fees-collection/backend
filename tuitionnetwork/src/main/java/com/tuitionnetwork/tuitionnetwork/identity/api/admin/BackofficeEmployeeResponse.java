package com.tuitionnetwork.tuitionnetwork.identity.api.admin;

import com.tuitionnetwork.tuitionnetwork.identity.domain.RoleCode;

import java.util.UUID;

public record BackofficeEmployeeResponse(
        UUID employeeId,
        String fullName,
        String branch,
        RoleCode role,
        UUID accountId,
        String username,
        String accountStatus
) {
}
