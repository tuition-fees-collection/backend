package com.tuitionnetwork.tuitionnetwork.identity.service;

import com.tuitionnetwork.tuitionnetwork.identity.domain.AuthenticatedActor;
import com.tuitionnetwork.tuitionnetwork.identity.domain.RoleCode;
import com.tuitionnetwork.tuitionnetwork.identity.infra.repository.GuardianStudentAccessRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import com.tuitionnetwork.tuitionnetwork.identity.domain.AuditAction;

import java.util.UUID;

@Service
public class IdentityAccessServiceImpl implements IdentityAccessService {
    private final CurrentActorProvider currentActorProvider;
    private final GuardianStudentAccessRepository guardianStudentAccessRepository;

    private final AuditService auditService;
    private final UUID onlineChannelEmployeeId;

    public IdentityAccessServiceImpl(
            CurrentActorProvider currentActorProvider,
            GuardianStudentAccessRepository guardianStudentAccessRepository,
            AuditService auditService,
            @Value("${payments.online-channel-employee-id}") UUID onlineChannelEmployeeId) {
        this.auditService = auditService;
        this.currentActorProvider = currentActorProvider;
        this.guardianStudentAccessRepository = guardianStudentAccessRepository;
        this.onlineChannelEmployeeId = onlineChannelEmployeeId;
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
    public void requireFeeSearchAccess() {
        AuthenticatedActor actor = currentActor();
        if (actor.role() == RoleCode.BANK_OFFICER) {
            return;
        }
        if (actor.role() == RoleCode.INSTITUTION_OFFICER && actor.institutionId() != null) {
            return;
        }
        deny(actor);
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
    public void requireInstitutionOfficer() {
        AuthenticatedActor actor = currentActor();
        if (actor.role() != RoleCode.INSTITUTION_OFFICER || actor.institutionId() == null) {
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

    @Override
    public void requireBackofficeStaff() {
        AuthenticatedActor actor = currentActor();
        if (actor.role() != RoleCode.ADMIN && actor.role() != RoleCode.BANK_OFFICER) {
            deny(actor);
        }
    }

    @Override
    public PaymentCaller authorizePaymentCreation(UUID studentId) {
        AuthenticatedActor actor = currentActor();
        if (studentId == null) {
            deny(actor);
        }
        if (actor.role() == RoleCode.PAYER) {
            requireStudentAccess(studentId);
            return new PaymentCaller(actor.actorId(), onlineChannelEmployeeId, RoleCode.PAYER);
        }
        if (actor.role() == RoleCode.BANK_OFFICER) {
            UUID guardianId = guardianStudentAccessRepository
                    .findFirstGuardianIdByStudentId(studentId)
                    .orElse(null);
            if (guardianId == null) {
                deny(actor);
            }
            return new PaymentCaller(guardianId, actor.actorId(), RoleCode.BANK_OFFICER);
        }
        deny(actor);
        throw new AccessDeniedException("Access denied");
    }

    @Override
    public void authorizePaymentAccess(UUID paymentGuardianId) {
        AuthenticatedActor actor = currentActor();
        if (actor.role() == RoleCode.BANK_OFFICER) {
            return;
        }
        if (actor.role() == RoleCode.PAYER
                && actor.actorId() != null
                && actor.actorId().equals(paymentGuardianId)) {
            return;
        }
        deny(actor);
    }

    @Override
    public void requireCashCollection() {
        requireBankOfficer();
    }

    private void deny(AuthenticatedActor actor) {
        auditService.recordAuthenticated(AuditAction.AUTHORIZATION_DENIED, actor);
        throw new AccessDeniedException("Access denied");
    }
}
