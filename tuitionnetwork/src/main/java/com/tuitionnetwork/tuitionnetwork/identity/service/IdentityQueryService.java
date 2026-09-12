package com.tuitionnetwork.tuitionnetwork.identity.service;

import com.tuitionnetwork.tuitionnetwork.identity.domain.NationalId;

import java.util.Set;
import java.util.UUID;

public interface IdentityQueryService {

    Set<UUID> getCurrentPayerStudentIds();

    GuardianSearchScope resolveGuardianSearchScope(
            NationalId nationalId
    );
}