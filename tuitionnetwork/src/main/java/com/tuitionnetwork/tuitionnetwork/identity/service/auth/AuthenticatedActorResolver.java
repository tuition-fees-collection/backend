package com.tuitionnetwork.tuitionnetwork.identity.service.auth;

import com.tuitionnetwork.tuitionnetwork.identity.domain.AuthAccount;
import com.tuitionnetwork.tuitionnetwork.identity.domain.AuthenticatedActor;
import com.tuitionnetwork.tuitionnetwork.identity.domain.ActorType;
import com.tuitionnetwork.tuitionnetwork.identity.domain.RoleCode;
import com.tuitionnetwork.tuitionnetwork.identity.infra.repository.ActorProfileLookupRepository;
import org.springframework.stereotype.Service;

@Service
public class AuthenticatedActorResolver {

    private final ActorProfileLookupRepository profileRepository;

    public AuthenticatedActorResolver(
            ActorProfileLookupRepository profileRepository
    ) {
        this.profileRepository = profileRepository;
    }

    public AuthenticatedActor resolve(AuthAccount account) {

        if (account.getGuardianId() != null) {
            return new AuthenticatedActor(
                    account.getId(),
                    account.getGuardianId(),
                    ActorType.GUARDIAN,
                    RoleCode.PAYER,
                    null,
                    null
            );
        }

        if (account.getBackofficeEmployeeId() != null) {

            var profile =
                    profileRepository
                            .findBackofficeProfile(
                                    account.getBackofficeEmployeeId()
                            )
                            .orElseThrow(
                                    () -> new IllegalStateException(
                                            "Backoffice profile not found"
                                    )
                            );

            RoleCode role =
                    RoleCode.valueOf(profile.roleName());

            if (role != RoleCode.BANK_OFFICER
                    && role != RoleCode.ADMIN) {

                throw new IllegalStateException(
                        "Invalid role for backoffice employee"
                );
            }

            return new AuthenticatedActor(
                    account.getId(),
                    profile.actorId(),
                    ActorType.BACKOFFICE_EMPLOYEE,
                    role,
                    null,
                    profile.branch()
            );
        }

        if (account.getInstitutionUserId() != null) {

            var profile =
                    profileRepository
                            .findInstitutionProfile(
                                    account.getInstitutionUserId()
                            )
                            .orElseThrow(
                                    () -> new IllegalStateException(
                                            "Institution profile not found"
                                    )
                            );

            RoleCode role =
                    RoleCode.valueOf(profile.roleName());

            if (role != RoleCode.INSTITUTION_OFFICER) {
                throw new IllegalStateException(
                        "Invalid role for institution user"
                );
            }

            return new AuthenticatedActor(
                    account.getId(),
                    profile.actorId(),
                    ActorType.INSTITUTION_USER,
                    role,
                    profile.institutionId(),
                    null
            );
        }

        throw new IllegalStateException(
                "Authentication account has no valid profile"
        );
    }
}