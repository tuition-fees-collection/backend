package com.tuitionnetwork.tuitionnetwork.identity.service;

import com.tuitionnetwork.tuitionnetwork.identity.domain.*;
import com.tuitionnetwork.tuitionnetwork.identity.infra.repository.GuardianStudentAccessRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.access.AccessDeniedException;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class IdentityAccessServiceImplTest {
    private CurrentActorProvider currentActorProvider;
    private GuardianStudentAccessRepository relationships;
    private IdentityAccessServiceImpl service;

    @BeforeEach
    void setUp() {
        currentActorProvider = mock(CurrentActorProvider.class);
        relationships = mock(GuardianStudentAccessRepository.class);
        service = new IdentityAccessServiceImpl(currentActorProvider, relationships);
    }

    @Test
    void shouldReturnCurrentActor() {
        var actor = useActor(RoleCode.PAYER, null);
        assertSame(actor, service.currentActor());
    }

    @Test
    void shouldAllowBankOfficer() {
        useActor(RoleCode.BANK_OFFICER, null);
        assertDoesNotThrow(service::requireBankOfficer);
    }

    @Test
    void shouldDenyOtherRolesBankOfficerAccess() {
        for (RoleCode role : new RoleCode[]{RoleCode.ADMIN, RoleCode.PAYER, RoleCode.INSTITUTION_OFFICER}) {
            useActor(role, null);
            assertThrows(AccessDeniedException.class, service::requireBankOfficer);
        }
        verifyNoInteractions(relationships);
    }

    @Test
    void shouldAllowOwnInstitution() {
        UUID institutionId = UUID.randomUUID();
        useActor(RoleCode.INSTITUTION_OFFICER, institutionId);
        assertDoesNotThrow(() -> service.requireInstitutionAccess(institutionId));
    }

    @Test
    void shouldDenyOtherInstitution() {
        useActor(RoleCode.INSTITUTION_OFFICER, UUID.randomUUID());
        assertThrows(AccessDeniedException.class,
                () -> service.requireInstitutionAccess(UUID.randomUUID()));
    }

    @Test
    void shouldDenyMissingInstitutionScope() {
        useActor(RoleCode.INSTITUTION_OFFICER, null);
        assertThrows(AccessDeniedException.class, () -> service.requireInstitutionAccess(null));
    }

    @Test
    void shouldDenyOtherRolesEvenWithMatchingInstitution() {
        UUID institutionId = UUID.randomUUID();
        for (RoleCode role : new RoleCode[]{RoleCode.ADMIN, RoleCode.PAYER, RoleCode.BANK_OFFICER}) {
            useActor(role, institutionId);
            assertThrows(AccessDeniedException.class, () -> service.requireInstitutionAccess(institutionId));
        }
    }

    @Test
    void shouldAllowRelatedStudentUsingAuthenticatedGuardianId() {
        var actor = useActor(RoleCode.PAYER, null);
        UUID studentId = UUID.randomUUID();
        when(relationships.existsRelationship(actor.actorId(), studentId)).thenReturn(true);
        assertDoesNotThrow(() -> service.requireStudentAccess(studentId));
        verify(relationships).existsRelationship(actor.actorId(), studentId);
        verifyNoMoreInteractions(relationships);
    }

    @Test
    void shouldDenyStudentBelongingOnlyToAnotherPayer() {
        var actor = useActor(RoleCode.PAYER, null);
        UUID studentId = UUID.randomUUID();
        // The requested student has no relationship with the current guardian.
        when(relationships.existsRelationship(actor.actorId(), studentId)).thenReturn(false);
        var error = assertThrows(AccessDeniedException.class, () -> service.requireStudentAccess(studentId));
        assertEquals("Access denied", error.getMessage());
        verify(relationships).existsRelationship(actor.actorId(), studentId);
    }

    @Test
    void shouldDenyNonPayerBeforeRelationshipLookup() {
        for (RoleCode role : new RoleCode[]{RoleCode.ADMIN, RoleCode.BANK_OFFICER, RoleCode.INSTITUTION_OFFICER}) {
            useActor(role, null);
            assertThrows(AccessDeniedException.class, () -> service.requireStudentAccess(UUID.randomUUID()));
        }
        verifyNoInteractions(relationships);
    }

    @Test
    void shouldDenyNullStudentBeforeRelationshipLookup() {
        useActor(RoleCode.PAYER, null);
        assertThrows(AccessDeniedException.class, () -> service.requireStudentAccess(null));
        verifyNoInteractions(relationships);
    }

    private AuthenticatedActor useActor(RoleCode role, UUID institutionId) {
        ActorType type = switch (role) {
            case PAYER -> ActorType.GUARDIAN;
            case INSTITUTION_OFFICER -> ActorType.INSTITUTION_USER;
            default -> ActorType.BACKOFFICE_EMPLOYEE;
        };
        var actor = new AuthenticatedActor(UUID.randomUUID(), UUID.randomUUID(), type, role, institutionId, null);
        when(currentActorProvider.currentActor()).thenReturn(actor);
        return actor;
    }
}
