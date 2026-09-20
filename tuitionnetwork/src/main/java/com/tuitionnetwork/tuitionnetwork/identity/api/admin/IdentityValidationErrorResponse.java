package com.tuitionnetwork.tuitionnetwork.identity.api.admin;

import java.util.Map;

public record IdentityValidationErrorResponse(
        String code,
        String message,
        Map<String, String> fieldErrors
) {
}
