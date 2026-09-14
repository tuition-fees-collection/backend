package com.tuitionnetwork.tuitionnetwork.identity.service;

import com.tuitionnetwork.tuitionnetwork.identity.domain.ActorType;
import com.tuitionnetwork.tuitionnetwork.identity.domain.AuthenticatedActor;
import com.tuitionnetwork.tuitionnetwork.identity.domain.NationalId;
import com.tuitionnetwork.tuitionnetwork.identity.domain.RoleCode;
import com.tuitionnetwork.tuitionnetwork.identity.infra.repository.GuardianIdentityLookupRepository;
import com.tuitionnetwork.tuitionnetwork.identity.infra.ports.MoiVerificationPort;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import com.tuitionnetwork.tuitionnetwork.identity.domain.AuditAction;

import java.util.Set;
import java.util.UUID;

@Service
public class IdentityQueryServiceImpl
        implements IdentityQueryService {

    private final IdentityAccessService identityAccessService;
    private final NationalIdHasher nationalIdHasher;
    private final GuardianIdentityLookupRepository guardianRepository;
    private final MoiVerificationPort moiVerificationPort;

    private final AuditService auditService;

    public IdentityQueryServiceImpl(
            IdentityAccessService identityAccessService,
            NationalIdHasher nationalIdHasher,
            GuardianIdentityLookupRepository guardianRepository,
            AuditService auditService,
            MoiVerificationPort moiVerificationPort
    ) {
        this.auditService = auditService;
        this.identityAccessService =
                identityAccessService;

        this.nationalIdHasher =
                nationalIdHasher;

        this.guardianRepository =
                guardianRepository;
        this.moiVerificationPort = moiVerificationPort;
    }

    @Override
    public Set<UUID> getCurrentPayerStudentIds() {

        AuthenticatedActor actor =
                identityAccessService.currentActor();

        if (actor.role() != RoleCode.PAYER
                || actor.actorType()
                   != ActorType.GUARDIAN) {

            auditService.recordAuthenticated(AuditAction.AUTHORIZATION_DENIED, actor);
            throw new AccessDeniedException(
                    "Access denied"
            );
        }

        return guardianRepository
                .findStudentIdsByGuardianId(
                        actor.actorId()
                );
    }

    @Override
    public GuardianSearchScope resolveGuardianSearchScope(
            NationalId nationalId
    ) {

        identityAccessService
                .requireBankOfficer();

        AuthenticatedActor actor = identityAccessService.currentActor();

        String nationalIdHmac =
                nationalIdHasher.hash(
                        nationalId
                );

        auditService.recordNationalIdLookup(actor, nationalIdHmac);

        GuardianIdentityLookupRepository.GuardianIdentity guardian =
                guardianRepository
                        .findGuardianByNationalIdHmac(
                                nationalIdHmac
                        )
                        .orElseThrow(
                                GuardianNotFoundException::new
                        );

        MoiVerificationPort.VerificationResult verification = moiVerificationPort.verify(
                new MoiVerificationPort.VerificationCommand(
                        nationalId.value(), guardian.fullName(), "GUARDIAN_FEE_LOOKUP"));
        if (!verification.valid()
                || verification.nameMatch() == null
                || !Boolean.TRUE.equals(verification.nameMatch().matched())) {
            throw new IdentityVerificationRejectedException();
        }

        Set<UUID> studentIds =
                guardianRepository
                        .findStudentIdsByGuardianId(
                                guardian.id()
                        );

        return new GuardianSearchScope(
                guardian.id(),
                studentIds
        );
    }
}
