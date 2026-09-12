package com.tuitionnetwork.tuitionnetwork.integration.infra.ports;

import java.util.UUID;


public interface StudentResolutionPort {

    UUID resolveRequiredStudentId(String nationalId);
}
