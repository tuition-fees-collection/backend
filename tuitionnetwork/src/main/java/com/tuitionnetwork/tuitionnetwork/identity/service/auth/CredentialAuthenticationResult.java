package com.tuitionnetwork.tuitionnetwork.identity.service.auth;

import com.tuitionnetwork.tuitionnetwork.identity.domain.AuthenticatedActor;

public record CredentialAuthenticationResult(
        boolean authenticated,
        AuthenticatedActor actor,
        Long tokenVersion,
        AuthenticationFailureReason failureReason
) {

    public static CredentialAuthenticationResult success(AuthenticatedActor actor, long tokenVersion) {
        return new CredentialAuthenticationResult(true, actor, tokenVersion, null);
    }

    public static CredentialAuthenticationResult failure(AuthenticationFailureReason reason) {
        return new CredentialAuthenticationResult(false, null, null, reason);
    }
}
