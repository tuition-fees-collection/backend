package com.tuitionnetwork.tuitionnetwork.identity.service;

import java.util.UUID;

public interface AdminAccountService {
    UUID createBackofficeAccount(String username, String rawPassword, UUID backofficeEmployeeId);
    UUID createInstitutionAccount(String username, String rawPassword, UUID institutionUserId);
    void deactivate(UUID accountId);
    void reactivate(UUID accountId);
}
