package com.tuitionnetwork.tuitionnetwork.identity.service;

import com.tuitionnetwork.tuitionnetwork.identity.domain.*;
import com.tuitionnetwork.tuitionnetwork.identity.infra.repository.GuardianIdentityLookupRepository;
import com.tuitionnetwork.tuitionnetwork.identity.infra.ports.MoiVerificationPort;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.access.AccessDeniedException;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class IdentityQueryServiceImplTest {
    private IdentityAccessService access;
    private NationalIdHasher hasher;
    private GuardianIdentityLookupRepository repository;
    private MoiVerificationPort moi;
    private IdentityQueryServiceImpl service;

    private AuditService auditService;

    @BeforeEach
    void setUp() {
        auditService = mock(AuditService.class);
        access = mock(IdentityAccessService.class);
        hasher = mock(NationalIdHasher.class);
        repository = mock(GuardianIdentityLookupRepository.class);
        moi = mock(MoiVerificationPort.class);
        service = new IdentityQueryServiceImpl(access, hasher, repository, auditService, moi);
    }

    @Test
    void payerReceivesOwnStudentIds() {
        UUID guardianId = UUID.randomUUID();
        Set<UUID> students = Set.of(UUID.randomUUID());
        when(access.currentActor()).thenReturn(new AuthenticatedActor(UUID.randomUUID(), guardianId,
                ActorType.GUARDIAN, RoleCode.PAYER, null, null));
        when(repository.findStudentIdsByGuardianId(guardianId)).thenReturn(students);
        assertEquals(students, service.getCurrentPayerStudentIds());
        verify(repository).findStudentIdsByGuardianId(guardianId);
        verifyNoInteractions(hasher, moi);
    }

    @Test
    void nonPayerIsDenied() {
        when(access.currentActor()).thenReturn(new AuthenticatedActor(UUID.randomUUID(), UUID.randomUUID(),
                ActorType.BACKOFFICE_EMPLOYEE, RoleCode.BANK_OFFICER, null, null));
        assertThrows(AccessDeniedException.class, service::getCurrentPayerStudentIds);
        verifyNoInteractions(hasher, repository, moi);
    }

    @Test
    void payerRoleWithWrongActorTypeIsDenied() {
        when(access.currentActor()).thenReturn(new AuthenticatedActor(UUID.randomUUID(), UUID.randomUUID(),
                ActorType.BACKOFFICE_EMPLOYEE, RoleCode.PAYER, null, null));
        assertThrows(AccessDeniedException.class, service::getCurrentPayerStudentIds);
        verifyNoInteractions(hasher, repository, moi);
    }

    @Test
    void bankOfficerResolvesGuardianUsingHmac() {
        var officer = new AuthenticatedActor(UUID.randomUUID(), UUID.randomUUID(),
                ActorType.BACKOFFICE_EMPLOYEE, RoleCode.BANK_OFFICER, null, null);
        when(access.currentActor()).thenReturn(officer);
        var id = new NationalId("29801011234567");
        String hash = "a".repeat(64);
        UUID guardianId = UUID.randomUUID();
        Set<UUID> students = Set.of(UUID.randomUUID());
        when(hasher.hash(id)).thenReturn(hash);
        when(repository.findGuardianByNationalIdHmac(hash)).thenReturn(Optional.of(
                new GuardianIdentityLookupRepository.GuardianIdentity(guardianId, "Mona Samir")));
        when(moi.verify(any())).thenReturn(verified(true, true));
        when(repository.findStudentIdsByGuardianId(guardianId)).thenReturn(students);
        assertEquals(new GuardianSearchScope(guardianId, students), service.resolveGuardianSearchScope(id));
        var order = inOrder(access, hasher, repository, auditService, moi);
        order.verify(access).requireBankOfficer();
        order.verify(access).currentActor();
        order.verify(hasher).hash(id);
        order.verify(auditService).recordNationalIdLookup(officer, hash);
        order.verify(repository).findGuardianByNationalIdHmac(hash);
        order.verify(moi).verify(new MoiVerificationPort.VerificationCommand(
                id.value(), "Mona Samir", "GUARDIAN_FEE_LOOKUP"));
        order.verify(repository).findStudentIdsByGuardianId(guardianId);
        verifyNoMoreInteractions(repository);
    }

    @Test
    void unauthorizedLookupIsDeniedBeforeHashingOrDatabaseAccess() {
        doThrow(new AccessDeniedException("Access denied")).when(access).requireBankOfficer();
        assertThrows(AccessDeniedException.class,
                () -> service.resolveGuardianSearchScope(new NationalId("29801011234567")));
        verifyNoInteractions(hasher, repository, moi);
    }

    @Test
    void unknownHmacProducesSafeNotFoundError() {
        var officer = new AuthenticatedActor(UUID.randomUUID(), UUID.randomUUID(),
                ActorType.BACKOFFICE_EMPLOYEE, RoleCode.BANK_OFFICER, null, null);
        when(access.currentActor()).thenReturn(officer);
        var id = new NationalId("29801011234567");
        String hash = "b".repeat(64);
        when(hasher.hash(id)).thenReturn(hash);
        when(repository.findGuardianByNationalIdHmac(hash)).thenReturn(Optional.empty());
        var error = assertThrows(GuardianNotFoundException.class, () -> service.resolveGuardianSearchScope(id));
        assertEquals("Guardian not found", error.getMessage());
        verify(auditService).recordNationalIdLookup(officer, hash);
        verify(repository).findGuardianByNationalIdHmac(hash);
        verifyNoMoreInteractions(repository);
        verifyNoInteractions(moi);
    }

    @Test
    void rejectedMoiVerificationStopsStudentDisclosure() {
        var officer = new AuthenticatedActor(UUID.randomUUID(), UUID.randomUUID(),
                ActorType.BACKOFFICE_EMPLOYEE, RoleCode.BANK_OFFICER, null, null);
        when(access.currentActor()).thenReturn(officer);
        var id = new NationalId("29801011234567");
        String hash = "c".repeat(64);
        UUID guardianId = UUID.randomUUID();
        when(hasher.hash(id)).thenReturn(hash);
        when(repository.findGuardianByNationalIdHmac(hash)).thenReturn(Optional.of(
                new GuardianIdentityLookupRepository.GuardianIdentity(guardianId, "Mona Samir")));
        when(moi.verify(any())).thenReturn(verified(false, false));

        var error = assertThrows(IdentityVerificationRejectedException.class,
                () -> service.resolveGuardianSearchScope(id));

        assertEquals("Guardian identity could not be verified", error.getMessage());
        verify(repository, never()).findStudentIdsByGuardianId(any());
    }

    private MoiVerificationPort.VerificationResult verified(boolean valid, boolean nameMatched) {
        return new MoiVerificationPort.VerificationResult(
                "moi-1", valid, null, "ACTIVE", null,
                new MoiVerificationPort.NameMatch("Mona Samir", nameMatched, null, null),
                java.util.Map.of(), java.util.List.of(), "GUARDIAN_FEE_LOOKUP", null, null);
    }
}
