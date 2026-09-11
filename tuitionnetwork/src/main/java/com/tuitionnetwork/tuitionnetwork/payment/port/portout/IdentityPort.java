package com.tuitionnetwork.tuitionnetwork.payment.port.portout;
import java.util.UUID;
public interface IdentityPort {
    boolean canAccessStudent(UUID guardianId, UUID studentId);
}