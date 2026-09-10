package com.tuitionnetwork.tuitionnetwork.identity.api.admin;

public record UpdateInstitutionRequest(
        String name,
        String type,
        String status
) {
}
