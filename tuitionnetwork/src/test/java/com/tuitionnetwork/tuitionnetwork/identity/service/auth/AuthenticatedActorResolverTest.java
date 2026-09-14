package com.tuitionnetwork.tuitionnetwork.identity.service.auth;

import com.tuitionnetwork.tuitionnetwork.identity.domain.AuthAccount;
import com.tuitionnetwork.tuitionnetwork.identity.domain.AuthenticatedActor;
import com.tuitionnetwork.tuitionnetwork.identity.domain.ActorType;
import com.tuitionnetwork.tuitionnetwork.identity.domain.RoleCode;
import com.tuitionnetwork.tuitionnetwork.identity.infra.repository.ActorProfileLookupRepository;
import com.tuitionnetwork.tuitionnetwork.identity.infra.repository.ActorProfileLookupRepository.BackofficeProfile;
import com.tuitionnetwork.tuitionnetwork.identity.infra.repository.ActorProfileLookupRepository.InstitutionProfile;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class AuthenticatedActorResolverTest {

    private ActorProfileLookupRepository profileRepository;
    private AuthenticatedActorResolver resolver;
    private UUID accountId;
    private UUID profileId;

    @BeforeEach
    void setUp() {
        profileRepository = mock(ActorProfileLookupRepository.class);
        resolver = new AuthenticatedActorResolver(profileRepository);
        accountId = UUID.randomUUID();
        profileId = UUID.randomUUID();
    }

    @Test
    void shouldResolveGuardianAsPayer() {
        AuthAccount account = withAccountId(
                AuthAccount.forGuardian("payer01", "{bcrypt}hash", profileId));

        AuthenticatedActor actor = resolver.resolve(account);

        assertEquals(new AuthenticatedActor(
                accountId, profileId, ActorType.GUARDIAN, RoleCode.PAYER, null, null), actor);
        verifyNoInteractions(profileRepository);
    }

    @Test
    void shouldPreserveBankOfficerBranch() {
        AuthAccount account = backofficeAccount();
        when(profileRepository.findBackofficeProfile(profileId))
                .thenReturn(Optional.of(new BackofficeProfile(profileId, "BANK_OFFICER", "Cairo")));

        AuthenticatedActor actor = resolver.resolve(account);

        assertEquals(new AuthenticatedActor(
                accountId, profileId, ActorType.BACKOFFICE_EMPLOYEE,
                RoleCode.BANK_OFFICER, null, "Cairo"), actor);
        verify(profileRepository).findBackofficeProfile(profileId);
        verifyNoMoreInteractions(profileRepository);
    }

    @Test
    void shouldResolveBackofficeAdmin() {
        AuthAccount account = backofficeAccount();
        when(profileRepository.findBackofficeProfile(profileId))
                .thenReturn(Optional.of(new BackofficeProfile(profileId, "ADMIN", "Head Office")));

        AuthenticatedActor actor = resolver.resolve(account);

        assertEquals(new AuthenticatedActor(
                accountId, profileId, ActorType.BACKOFFICE_EMPLOYEE,
                RoleCode.ADMIN, null, "Head Office"), actor);
    }

    @Test
    void shouldPreserveInstitutionScope() {
        AuthAccount account = institutionAccount();
        UUID institutionId = UUID.randomUUID();
        when(profileRepository.findInstitutionProfile(profileId))
                .thenReturn(Optional.of(new InstitutionProfile(
                        profileId, institutionId, "INSTITUTION_OFFICER")));

        AuthenticatedActor actor = resolver.resolve(account);

        assertEquals(new AuthenticatedActor(
                accountId, profileId, ActorType.INSTITUTION_USER,
                RoleCode.INSTITUTION_OFFICER, institutionId, null), actor);
        verify(profileRepository).findInstitutionProfile(profileId);
        verifyNoMoreInteractions(profileRepository);
    }

    @Test
    void shouldRejectInvalidBackofficeRole() {
        AuthAccount account = backofficeAccount();
        when(profileRepository.findBackofficeProfile(profileId))
                .thenReturn(Optional.of(new BackofficeProfile(profileId, "PAYER", "Cairo")));

        assertThrows(IllegalStateException.class, () -> resolver.resolve(account));
    }

    @Test
    void shouldRejectMissingBackofficeProfile() {
        AuthAccount account = backofficeAccount();
        when(profileRepository.findBackofficeProfile(profileId)).thenReturn(Optional.empty());

        assertThrows(IllegalStateException.class, () -> resolver.resolve(account));
    }

    @Test
    void shouldRejectMissingInstitutionProfile() {
        AuthAccount account = institutionAccount();
        when(profileRepository.findInstitutionProfile(profileId)).thenReturn(Optional.empty());

        assertThrows(IllegalStateException.class, () -> resolver.resolve(account));
    }

    private AuthAccount backofficeAccount() {
        return withAccountId(AuthAccount.forBackofficeEmployee("officer01", "{bcrypt}hash", profileId));
    }

    private AuthAccount institutionAccount() {
        return withAccountId(AuthAccount.forInstitutionUser("staff01", "{bcrypt}hash", profileId));
    }

    private AuthAccount withAccountId(AuthAccount account) {
        // Simulate the ID JPA assigns, without starting a database or adding a production setter.
        ReflectionTestUtils.setField(account, "id", accountId);
        return account;
    }
}
