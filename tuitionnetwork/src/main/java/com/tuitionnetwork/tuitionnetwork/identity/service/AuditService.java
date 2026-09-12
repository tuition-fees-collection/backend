package com.tuitionnetwork.tuitionnetwork.identity.service;

import com.tuitionnetwork.tuitionnetwork.identity.domain.AuditAction;
import com.tuitionnetwork.tuitionnetwork.identity.domain.AuthenticatedActor;

public interface AuditService {

    void recordAuthenticated(
            AuditAction action,
            AuthenticatedActor actor
    );

    void recordNationalIdLookup(
            AuthenticatedActor actor,
            String nationalIdHmac
    );

    void recordUnauthenticated(
            AuditAction action
    );
}
