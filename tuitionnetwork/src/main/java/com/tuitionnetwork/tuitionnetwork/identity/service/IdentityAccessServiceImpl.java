package com.tuitionnetwork.tuitionnetwork.identity.service;

import com.tuitionnetwork.tuitionnetwork.identity.domain.AuthenticatedActor;
import com.tuitionnetwork.tuitionnetwork.identity.domain.RoleCode;
import com.tuitionnetwork.tuitionnetwork.identity.infra.repository.GuardianStudentAccessRepository;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
public class IdentityAccessServiceImpl implements IdentityAccessService {
    private final CurrentActorProvider currentActorProvider;
    private final GuardianStudentAccessRepository guardianStudentAccessRepository;

    public IdentityAccessServiceImpl(
            CurrentActorProvider currentActorProvider,
            GuardianStudentAccessRepository guardianStudentAccessRepository) {
        this.currentActorProvider = currentActorProvider;
        this.guardianStudentAccessRepository = guardianStudentAccessRepository;
    }

    @Override
    public AuthenticatedActor currentActor() {
        return currentActorProvider.currentActor();
    }

    @Override
    public void requireBankOfficer() {
        if (currentActor().role() != RoleCode.BANK_OFFICER) {
            throw new AccessDeniedException("Access denied");
        }
    }

    @Override
    public void requireInstitutionAccess(UUID institutionId) {
        AuthenticatedActor actor = currentActor();
        if (actor.role() != RoleCode.INSTITUTION_OFFICER
                || actor.institutionId() == null
                || !actor.institutionId().equals(institutionId)) {
            throw new AccessDeniedException("Access denied");
        }
    }

    @Override
    public void requireStudentAccess(UUID studentId) {
        AuthenticatedActor actor = currentActor();
        if (actor.role() != RoleCode.PAYER || actor.actorId() == null || studentId == null) {
            throw new AccessDeniedException("Access denied");
        }
        if (!guardianStudentAccessRepository.existsRelationship(actor.actorId(), studentId)) {
            throw new AccessDeniedException("Access denied");
        }
    }
}
