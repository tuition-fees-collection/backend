package com.tuitionnetwork.tuitionnetwork.identity.api.admin;

import java.util.UUID;

public record InstitutionResponse(
        UUID id,
        String name,
        String type,
        String status
) {
}
