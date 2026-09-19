package com.tuitionnetwork.tuitionnetwork.identity.service;

import com.tuitionnetwork.tuitionnetwork.identity.api.admin.*;
import com.tuitionnetwork.tuitionnetwork.identity.domain.*;
import com.tuitionnetwork.tuitionnetwork.identity.infra.repository.*;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.*;

@Service
public class InstitutionAdminServiceImpl implements InstitutionAdminService {
    private final IdentityAccessService access;
    private final InstitutionAdminRepository repository;
    private final AuthAccountRepository accounts;
    private final PasswordEncoder encoder;
    private final AuditService audit;

    public InstitutionAdminServiceImpl(IdentityAccessService access, InstitutionAdminRepository repository,
                                      AuthAccountRepository accounts, PasswordEncoder encoder, AuditService audit) {
        this.access = access; this.repository = repository; this.accounts = accounts;
        this.encoder = encoder; this.audit = audit;
    }
    @Override
    @Transactional(readOnly = true)
    public List<InstitutionResponse> getInstitutions() {
        access.requireBackofficeStaff();
        return repository.findAllInstitutions();
    }
    @Override
    @Transactional
    public InstitutionResponse createInstitution(CreateInstitutionRequest request) {
        access.requireAdmin();
        if (request == null) throw new IllegalArgumentException("Request required");
        UUID id = repository.createInstitution(required(request.name(), 255), required(request.type(), 50),
                required(request.status(), 20), access.currentActor().actorId());
        audit.recordAuthenticated(AuditAction.INSTITUTION_CREATED, access.currentActor());
        return repository.findInstitutionById(id).orElseThrow();
    }
    @Override
    @Transactional
    public InstitutionResponse updateInstitution(UUID id, UpdateInstitutionRequest request) {
        access.requireAdmin();
        requireInstitution(id);
        if (request == null) throw new IllegalArgumentException("Request required");
        repository.updateInstitution(id, optional(request.name(), 255), optional(request.type(), 50),
                optional(request.status(), 20));
        audit.recordAuthenticated(AuditAction.INSTITUTION_UPDATED, access.currentActor());
        return repository.findInstitutionById(id).orElseThrow();
    }
    @Override
    @Transactional(readOnly = true)
    public List<InstitutionUserResponse> getInstitutionUsers(UUID id) {
        access.requireAdmin();
        requireInstitution(id);
        return repository.findUsers(id);
    }
    @Override
    @Transactional
    public InstitutionUserResponse createInstitutionUser(UUID id, CreateInstitutionUserRequest request) {
        access.requireAdmin();
        requireInstitution(id);
        if (request == null) throw new IllegalArgumentException("Request required");
        String name = required(request.fullName(), 255);
        String username = required(request.username(), 150).toLowerCase(Locale.ROOT);
        if (request.password() == null || request.password().isBlank()) throw new IllegalArgumentException("Password required");
        if (accounts.existsByUsernameNormalized(username)) throw new IllegalArgumentException("Username already exists");
        UUID roleId = repository.findInstitutionOfficerRoleId().orElseThrow(
                () -> new IllegalStateException("Institution officer role not configured"));
        UUID userId = repository.createInstitutionUser(id, name, roleId);
        var account = accounts.saveAndFlush(AuthAccount.forInstitutionUser(username, encoder.encode(request.password()), userId));
        audit.recordAuthenticated(AuditAction.STAFF_CREATED, access.currentActor());
        return new InstitutionUserResponse(userId, id, name, RoleCode.INSTITUTION_OFFICER.name(),
                account.getId(), username, account.getStatus().name());
    }
    private void requireInstitution(UUID id) {
        if (id == null || !repository.institutionExists(id)) throw new IllegalArgumentException("Institution not found");
    }
    private String optional(String value, int max) { return value == null ? null : required(value, max); }
    private String required(String value, int max) {
        if (value == null || value.isBlank() || value.trim().length() > max)
            throw new IllegalArgumentException("Invalid institution request");
        return value.trim();
    }
}
