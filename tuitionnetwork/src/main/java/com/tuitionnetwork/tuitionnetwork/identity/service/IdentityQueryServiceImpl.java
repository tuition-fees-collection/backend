package com.tuitionnetwork.tuitionnetwork.identity.service;

import com.tuitionnetwork.tuitionnetwork.identity.domain.ActorType;
import com.tuitionnetwork.tuitionnetwork.identity.domain.AuthenticatedActor;
import com.tuitionnetwork.tuitionnetwork.identity.domain.NationalId;
import com.tuitionnetwork.tuitionnetwork.identity.domain.RoleCode;
import com.tuitionnetwork.tuitionnetwork.identity.infra.repository.GuardianIdentityLookupRepository;
import com.tuitionnetwork.tuitionnetwork.identity.infra.repository.StudentIdentityLookupRepository;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import com.tuitionnetwork.tuitionnetwork.identity.domain.AuditAction;

import java.util.Collection;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

@Service
public class IdentityQueryServiceImpl
        implements IdentityQueryService {

    private final IdentityAccessService identityAccessService;
    private final NationalIdHasher nationalIdHasher;
    private final GuardianIdentityLookupRepository guardianRepository;
    private final StudentIdentityLookupRepository studentRepository;

    private final AuditService auditService;

    public IdentityQueryServiceImpl(
            IdentityAccessService identityAccessService,
            NationalIdHasher nationalIdHasher,
            GuardianIdentityLookupRepository guardianRepository,
            StudentIdentityLookupRepository studentRepository,
                AuditService auditService
    ) {
        this.auditService = auditService;
        this.identityAccessService =
                identityAccessService;

        this.nationalIdHasher =
                nationalIdHasher;

        this.guardianRepository =
                guardianRepository;
        this.studentRepository = studentRepository;
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
                .requireFeeSearchAccess();

        AuthenticatedActor actor = identityAccessService.currentActor();

        String nationalIdHmac =
                nationalIdHasher.hash(
                        nationalId
                );

        auditService.recordNationalIdLookup(actor, nationalIdHmac);

        UUID guardianId =
                guardianRepository
                        .findGuardianIdByNationalIdHmac(
                                nationalIdHmac
                        )
                        .orElseThrow(
                                GuardianNotFoundException::new
                        );

        Set<UUID> studentIds =
                guardianRepository
                        .findStudentIdsByGuardianId(
                                guardianId
                        );

        return new GuardianSearchScope(
                guardianId,
                studentIds
        );
    }

    @Override
    public Map<UUID, String> findStudentNames(Collection<UUID> studentIds) {
        return studentRepository.findFullNamesByIds(studentIds);
    }
}
