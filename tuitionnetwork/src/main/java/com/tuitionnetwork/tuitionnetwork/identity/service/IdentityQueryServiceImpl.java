package com.tuitionnetwork.tuitionnetwork.identity.service;

import com.tuitionnetwork.tuitionnetwork.identity.domain.ActorType;
import com.tuitionnetwork.tuitionnetwork.identity.domain.AuthenticatedActor;
import com.tuitionnetwork.tuitionnetwork.identity.domain.NationalId;
import com.tuitionnetwork.tuitionnetwork.identity.domain.RoleCode;
import com.tuitionnetwork.tuitionnetwork.identity.infra.repository.GuardianIdentityLookupRepository;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;

import java.util.Set;
import java.util.UUID;

@Service
public class IdentityQueryServiceImpl
        implements IdentityQueryService {

    private final IdentityAccessService identityAccessService;
    private final NationalIdHasher nationalIdHasher;
    private final GuardianIdentityLookupRepository guardianRepository;

    public IdentityQueryServiceImpl(
            IdentityAccessService identityAccessService,
            NationalIdHasher nationalIdHasher,
            GuardianIdentityLookupRepository guardianRepository
    ) {
        this.identityAccessService =
                identityAccessService;

        this.nationalIdHasher =
                nationalIdHasher;

        this.guardianRepository =
                guardianRepository;
    }

    @Override
    public Set<UUID> getCurrentPayerStudentIds() {

        AuthenticatedActor actor =
                identityAccessService.currentActor();

        if (actor.role() != RoleCode.PAYER
                || actor.actorType()
                   != ActorType.GUARDIAN) {

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

        String nationalIdHmac =
                nationalIdHasher.hash(
                        nationalId
                );

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
}
