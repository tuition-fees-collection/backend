package com.tuitionnetwork.tuitionnetwork.integration.infra.ports;

import java.util.UUID;


public interface StudentResolutionPort {

    default boolean isAvailable() {
        return true;
    }

    UUID resolveRequiredStudentId(String nationalId);
}
