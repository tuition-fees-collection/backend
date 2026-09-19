package com.tuitionnetwork.tuitionnetwork.identity.service;

import com.tuitionnetwork.tuitionnetwork.identity.domain.NationalId;

import java.util.Collection;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public interface IdentityQueryService {

    Set<UUID> getCurrentPayerStudentIds();

    GuardianSearchScope resolveGuardianSearchScope(
            NationalId nationalId
    );

    Map<UUID, String> findStudentNames(Collection<UUID> studentIds);
}