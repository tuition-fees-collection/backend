package com.tuitionnetwork.tuitionnetwork.identity.service;

import java.util.UUID;

import com.tuitionnetwork.tuitionnetwork.identity.domain.AuthenticatedActor;

public interface IdentityAccessService {

    AuthenticatedActor currentActor();

    void requireBankOfficer();

    void requireInstitutionAccess(UUID institutionId);

    void requireStudentAccess(UUID studentId);

}
