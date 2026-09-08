package com.tuitionnetwork.tuitionnetwork.identity.service;

import java.util.Set;
import java.util.UUID;

public record GuardianSearchScope(
        UUID guardianId,
        Set<UUID> studentIds
) {


}
