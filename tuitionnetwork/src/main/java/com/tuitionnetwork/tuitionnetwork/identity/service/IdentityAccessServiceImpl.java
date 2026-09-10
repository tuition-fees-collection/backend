package com.tuitionnetwork.tuitionnetwork.identity.service;

import com.tuitionnetwork.tuitionnetwork.identity.domain.AuthenticatedActor;
import com.tuitionnetwork.tuitionnetwork.identity.domain.RoleCode;
import com.tuitionnetwork.tuitionnetwork.identity.infra.repository.GuardianStudentAccessRepository;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import com.tuitionnetwork.tuitionnetwork.identity.domain.AuditAction;

import java.util.UUID;

@Service
public class IdentityAccessServiceImpl implements IdentityAccessService {
    private final CurrentActorProvider currentActorProvider;
    private final GuardianStudentAccessRepository guardianStudentAccessRepository;

    private final AuditService auditService;

    public IdentityAccessServiceImpl(
            CurrentActorProvider currentActorProvider,
            GuardianStudentAccessRepository guardianStudentAccessRepository, AuditService auditService) {
        this.auditService = auditService;
        this.currentActorProvider = currentActorProvider;
        this.guardianStudentAccessRepository = guardianStudentAccessRepository;
    }

    @Override
    public AuthenticatedActor currentActor() {
        return currentActorProvider.currentActor();
    }

    @Override
    public void requireBankOfficer() {
        AuthenticatedActor actor = currentActor();
        if (actor.role() != RoleCode.BANK_OFFICER) {
            deny(actor);
        }
    }

    @Override
    public void requireInstitutionAccess(UUID institutionId) {
        AuthenticatedActor actor = currentActor();
        if (actor.role() != RoleCode.INSTITUTION_OFFICER
                || actor.institutionId() == null
                || !actor.institutionId().equals(institutionId)) {
            deny(actor);
        }
    }

    @Override
    public void requireStudentAccess(UUID studentId) {
        AuthenticatedActor actor = currentActor();
        if (actor.role() != RoleCode.PAYER || actor.actorId() == null || studentId == null) {
            deny(actor);
        }
        if (!guardianStudentAccessRepository.existsRelationship(actor.actorId(), studentId)) {
            deny(actor);
        }
    }
    @Override
    public void requireAdmin() {
        AuthenticatedActor actor = currentActor();
        if (actor.role() != RoleCode.ADMIN) {
            deny(actor);
        }
    }

    private void deny(AuthenticatedActor actor) {
        auditService.recordAuthenticated(AuditAction.AUTHORIZATION_DENIED, actor);
        throw new AccessDeniedException("Access denied");
    }
}
