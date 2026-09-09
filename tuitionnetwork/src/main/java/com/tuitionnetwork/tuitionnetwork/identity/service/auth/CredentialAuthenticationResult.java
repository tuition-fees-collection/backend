package com.tuitionnetwork.tuitionnetwork.identity.service.auth;

import java.util.UUID;

public record CredentialAuthenticationResult(
        boolean authenticated,
        UUID accountId,
        AuthenticationFailureReason failureReason
) {

    public static CredentialAuthenticationResult success(
            UUID accountId
    ) {
        return new CredentialAuthenticationResult(
                true,
                accountId,
                null
        );
    }

    public static CredentialAuthenticationResult failure(
            AuthenticationFailureReason reason
    ) {
        return new CredentialAuthenticationResult(
                false,
                null,
                reason
        );
    }
}