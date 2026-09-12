package com.tuitionnetwork.tuitionnetwork.identity.service;

import com.tuitionnetwork.tuitionnetwork.identity.domain.*;
import com.tuitionnetwork.tuitionnetwork.identity.infra.repository.AuthAccountRepository;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Locale;
import java.util.UUID;

@Service
public class AdminAccountServiceImpl implements AdminAccountService {
    private final IdentityAccessService access;
    private final AuthAccountRepository repository;
    private final PasswordEncoder encoder;
    private final AuditService audit;

    public AdminAccountServiceImpl(IdentityAccessService access, AuthAccountRepository repository,
                                   PasswordEncoder encoder, AuditService audit) {
        this.access = access;
        this.repository = repository;
        this.encoder = encoder;
        this.audit = audit;
    }

    @Override
    @Transactional
    public UUID createBackofficeAccount(String username, String rawPassword, UUID backofficeEmployeeId) {
        access.requireAdmin();
        validateCredentials(username, rawPassword, backofficeEmployeeId);
        var account = AuthAccount.forBackofficeEmployee(normalizeUsername(username),
                encoder.encode(rawPassword), backofficeEmployeeId);
        var saved = repository.saveAndFlush(account);
        audit.recordAuthenticated(AuditAction.STAFF_CREATED, access.currentActor());
        return saved.getId();
    }

    @Override
    @Transactional
    public UUID createInstitutionAccount(String username, String rawPassword, UUID institutionUserId) {
        access.requireAdmin();
        validateCredentials(username, rawPassword, institutionUserId);
        var account = AuthAccount.forInstitutionUser(normalizeUsername(username),
                encoder.encode(rawPassword), institutionUserId);
        var saved = repository.saveAndFlush(account);
        audit.recordAuthenticated(AuditAction.STAFF_CREATED, access.currentActor());
        return saved.getId();
    }

    @Override
    @Transactional
    public void deactivate(UUID accountId) {
        access.requireAdmin();
        var target = staffAccount(accountId);
        target.deactivate();
        repository.flush();
        audit.recordAuthenticated(AuditAction.STAFF_DEACTIVATED, access.currentActor());
    }

    @Override
    @Transactional
    public void reactivate(UUID accountId) {
        access.requireAdmin();
        var target = staffAccount(accountId);
        target.activate();
        repository.flush();
        audit.recordAuthenticated(AuditAction.STAFF_REACTIVATED, access.currentActor());
    }

    private AuthAccount staffAccount(UUID id) {
        if (id == null) throw new IllegalArgumentException("Account is required");
        var account = repository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Account not found"));
        if (!account.isStaffAccount()) {
            audit.recordAuthenticated(AuditAction.AUTHORIZATION_DENIED, access.currentActor());
            throw new AccessDeniedException("Access denied");
        }
        return account;
    }

    private void validateCredentials(String username, String password, UUID profileId) {
        normalizeUsername(username);
        if (password == null || password.isBlank() || profileId == null) {
            throw new IllegalArgumentException("Password and staff profile are required");
        }
    }

    private String normalizeUsername(String username) {
        if (username == null || username.isBlank() || username.trim().length() > 150) {
            throw new IllegalArgumentException("Valid username is required");
        }
        return username.trim().toLowerCase(Locale.ROOT);
    }
}
