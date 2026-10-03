package com.tuitionnetwork.tuitionnetwork.identity.service;

import com.tuitionnetwork.tuitionnetwork.identity.domain.*;
import com.tuitionnetwork.tuitionnetwork.identity.infra.repository.GuardianStudentAccessRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import com.tuitionnetwork.tuitionnetwork.identity.domain.AuditAction;
import org.springframework.security.access.AccessDeniedException;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class IdentityAccessServiceImplTest {
    private static final UUID ONLINE_CHANNEL = UUID.fromString("22222222-2222-2222-2222-000000000000");

    private CurrentActorProvider currentActorProvider;
    private GuardianStudentAccessRepository relationships;
    private IdentityAccessServiceImpl service;

    private AuditService auditService;

    @BeforeEach
    void setUp() {
        auditService = mock(AuditService.class);
        currentActorProvider = mock(CurrentActorProvider.class);
        relationships = mock(GuardianStudentAccessRepository.class);
        service = new IdentityAccessServiceImpl(
                currentActorProvider, relationships, auditService, ONLINE_CHANNEL);
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
        verifyNoInteractions(auditService);
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
    void shouldAllowBankOfficerFeeSearch() {
        useActor(RoleCode.BANK_OFFICER, null);
        assertDoesNotThrow(service::requireFeeSearchAccess);
        verifyNoInteractions(auditService);
    }

    @Test
    void shouldAllowInstitutionOfficerFeeSearchForOwnInstitution() {
        useActor(RoleCode.INSTITUTION_OFFICER, UUID.randomUUID());
        assertDoesNotThrow(service::requireFeeSearchAccess);
        verifyNoInteractions(auditService);
    }

    @Test
    void shouldDenyInstitutionOfficerFeeSearchWithoutInstitution() {
        var actor = useActor(RoleCode.INSTITUTION_OFFICER, null);
        assertThrows(AccessDeniedException.class, service::requireFeeSearchAccess);
        verify(auditService).recordAuthenticated(AuditAction.AUTHORIZATION_DENIED, actor);
    }

    @Test
    void shouldDenyPayerAndAdminFeeSearch() {
        for (RoleCode role : new RoleCode[]{RoleCode.ADMIN, RoleCode.PAYER}) {
            var actor = useActor(role, null);
            assertThrows(AccessDeniedException.class, service::requireFeeSearchAccess);
            verify(auditService).recordAuthenticated(AuditAction.AUTHORIZATION_DENIED, actor);
        }
    }

    @Test
    void shouldAllowOwnInstitution() {
        UUID institutionId = UUID.randomUUID();
        useActor(RoleCode.INSTITUTION_OFFICER, institutionId);
        assertDoesNotThrow(() -> service.requireInstitutionAccess(institutionId));
        verifyNoInteractions(auditService);
    }

    @Test
    void shouldDenyOtherInstitution() {
        var actor = useActor(RoleCode.INSTITUTION_OFFICER, UUID.randomUUID());
        assertThrows(AccessDeniedException.class,
                () -> service.requireInstitutionAccess(UUID.randomUUID()));
        verify(auditService).recordAuthenticated(AuditAction.AUTHORIZATION_DENIED, actor);
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
        verifyNoInteractions(auditService);
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
        verify(auditService).recordAuthenticated(AuditAction.AUTHORIZATION_DENIED, actor);
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

    @Test
    void payerPaymentCreationUsesAuthenticatedGuardianAndOnlineChannel() {
        var actor = useActor(RoleCode.PAYER, null);
        UUID studentId = UUID.randomUUID();
        when(relationships.existsRelationship(actor.actorId(), studentId)).thenReturn(true);

        var caller = service.authorizePaymentCreation(studentId);

        assertEquals(actor.actorId(), caller.guardianId());
        assertEquals(ONLINE_CHANNEL, caller.processedBy());
        assertEquals(RoleCode.PAYER, caller.role());
    }

    @Test
    void bankOfficerPaymentCreationResolvesGuardianFromStudent() {
        var actor = useActor(RoleCode.BANK_OFFICER, null);
        UUID studentId = UUID.randomUUID();
        UUID guardianId = UUID.randomUUID();
        when(relationships.findFirstGuardianIdByStudentId(studentId)).thenReturn(java.util.Optional.of(guardianId));

        var caller = service.authorizePaymentCreation(studentId);

        assertEquals(guardianId, caller.guardianId());
        assertEquals(actor.actorId(), caller.processedBy());
        assertEquals(RoleCode.BANK_OFFICER, caller.role());
        verifyNoInteractions(auditService);
    }

    @Test
    void adminCannotCreateAPayment() {
        useActor(RoleCode.ADMIN, null);
        assertThrows(AccessDeniedException.class, () -> service.authorizePaymentCreation(UUID.randomUUID()));
    }

    @Test
    void payerCanAccessOwnPaymentAndNotSomeoneElses() {
        var actor = useActor(RoleCode.PAYER, null);
        assertDoesNotThrow(() -> service.authorizePaymentAccess(actor.actorId()));
        assertThrows(AccessDeniedException.class, () -> service.authorizePaymentAccess(UUID.randomUUID()));
    }

    @Test
    void bankOfficerCanAccessAnyPayment() {
        useActor(RoleCode.BANK_OFFICER, null);
        assertDoesNotThrow(() -> service.authorizePaymentAccess(UUID.randomUUID()));
    }

    @Test
    void onlyBankOfficerCanCollectCash() {
        useActor(RoleCode.BANK_OFFICER, null);
        assertDoesNotThrow(service::requireCashCollection);
        useActor(RoleCode.PAYER, null);
        assertThrows(AccessDeniedException.class, service::requireCashCollection);
    }

    @Test
    void shouldAllowBackofficeStaff() {
        useActor(RoleCode.ADMIN, null);
        assertDoesNotThrow(service::requireBackofficeStaff);
        useActor(RoleCode.BANK_OFFICER, null);
        assertDoesNotThrow(service::requireBackofficeStaff);
        verifyNoInteractions(auditService);
    }

    @Test
    void shouldDenyPayerAndInstitutionOfficerBackofficeStaffAccess() {
        for (RoleCode role : new RoleCode[]{RoleCode.PAYER, RoleCode.INSTITUTION_OFFICER}) {
            var actor = useActor(role, UUID.randomUUID());
            assertThrows(AccessDeniedException.class, service::requireBackofficeStaff);
            verify(auditService).recordAuthenticated(AuditAction.AUTHORIZATION_DENIED, actor);
        }
    }

    @Test
    void institutionOfficerWithSchoolCanUseChatbot() {
        useActor(RoleCode.INSTITUTION_OFFICER, UUID.randomUUID());
        assertDoesNotThrow(service::requireInstitutionOfficer);
        verifyNoInteractions(auditService);
    }

    @Test
    void chatbotDeniesEveryRoleExceptAnInstitutionOfficerBoundToASchool() {
        var officerWithoutSchool = useActor(RoleCode.INSTITUTION_OFFICER, null);
        assertThrows(AccessDeniedException.class, service::requireInstitutionOfficer);
        verify(auditService).recordAuthenticated(AuditAction.AUTHORIZATION_DENIED, officerWithoutSchool);

        for (RoleCode role : new RoleCode[]{RoleCode.ADMIN, RoleCode.PAYER, RoleCode.BANK_OFFICER}) {
            var actor = useActor(role, UUID.randomUUID());
            assertThrows(AccessDeniedException.class, service::requireInstitutionOfficer);
            verify(auditService).recordAuthenticated(AuditAction.AUTHORIZATION_DENIED, actor);
        }
    }

    @Test
    void onlyAdminCanManageStaff() {
        useActor(RoleCode.ADMIN, null);
        assertDoesNotThrow(service::requireAdmin);
        verifyNoInteractions(auditService);
        for (RoleCode role : new RoleCode[]{RoleCode.PAYER, RoleCode.BANK_OFFICER, RoleCode.INSTITUTION_OFFICER}) {
            var actor = useActor(role, null);
            assertThrows(AccessDeniedException.class, service::requireAdmin);
            verify(auditService).recordAuthenticated(AuditAction.AUTHORIZATION_DENIED, actor);
        }
    }
}
