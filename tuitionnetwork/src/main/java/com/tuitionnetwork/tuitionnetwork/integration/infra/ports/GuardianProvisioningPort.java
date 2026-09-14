package com.tuitionnetwork.tuitionnetwork.integration.infra.ports;

import java.util.UUID;


public interface GuardianProvisioningPort {

    UUID upsertGuardianStub(String nationalId, String fullName);

    void linkGuardianToStudent(UUID guardianId, UUID studentId);
}
