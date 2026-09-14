package com.tuitionnetwork.tuitionnetwork.identity.service.auth;

import com.tuitionnetwork.tuitionnetwork.identity.domain.AuthenticatedActor;

public interface TokenIssuer {

    String issue(
            AuthenticatedActor actor,
            long tokenVersion
    );
}