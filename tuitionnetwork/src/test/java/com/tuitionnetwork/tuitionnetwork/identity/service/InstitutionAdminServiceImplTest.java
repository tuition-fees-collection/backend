package com.tuitionnetwork.tuitionnetwork.identity.service;

import com.tuitionnetwork.tuitionnetwork.identity.api.admin.*;
import com.tuitionnetwork.tuitionnetwork.identity.domain.*;
import com.tuitionnetwork.tuitionnetwork.identity.infra.repository.*;
import org.junit.jupiter.api.Test;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.crypto.password.PasswordEncoder;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class InstitutionAdminServiceImplTest {
    private final IdentityAccessService access = mock(IdentityAccessService.class);
    private final InstitutionAdminRepository repository = mock(InstitutionAdminRepository.class);
    private final AuthAccountRepository accounts = mock(AuthAccountRepository.class);
    private final PasswordEncoder encoder = mock(PasswordEncoder.class);
    private final AuditService audit = mock(AuditService.class);
    private final InstitutionAdminService service = new InstitutionAdminServiceImpl(access, repository, accounts, encoder, audit);

    @Test void nonAdminCannotCallAnyOperation() {
        doThrow(new AccessDeniedException("Access denied")).when(access).requireAdmin();
        doThrow(new AccessDeniedException("Access denied")).when(access).requireBackofficeStaff();
        assertThrows(AccessDeniedException.class, service::getInstitutions);
        assertThrows(AccessDeniedException.class, () -> service.createInstitution(null));
        assertThrows(AccessDeniedException.class, () -> service.updateInstitution(UUID.randomUUID(), null));
        assertThrows(AccessDeniedException.class, () -> service.getInstitutionUsers(UUID.randomUUID()));
        assertThrows(AccessDeniedException.class, () -> service.createInstitutionUser(UUID.randomUUID(), null));
        verifyNoInteractions(repository, accounts, encoder, audit);
    }
    @Test void adminCanListInstitutions() {
        when(repository.findAllInstitutions()).thenReturn(List.of());
        assertEquals(List.of(), service.getInstitutions());
        verify(access).requireBackofficeStaff();
    }
    @Test void creationUsesAuthenticatedAdminProfileId() {
        UUID actorId = UUID.randomUUID();
        UUID id = UUID.randomUUID();
        when(access.currentActor()).thenReturn(new AuthenticatedActor(UUID.randomUUID(), actorId,
                ActorType.BACKOFFICE_EMPLOYEE, RoleCode.ADMIN, null, null));
        when(repository.createInstitution("School", "SCHOOL", "ACTIVE", actorId)).thenReturn(id);
        var expected = new InstitutionResponse(id, "School", "SCHOOL", "ACTIVE");
        when(repository.findInstitutionById(id)).thenReturn(Optional.of(expected));
        assertEquals(expected, service.createInstitution(new CreateInstitutionRequest(" School ", "SCHOOL", "ACTIVE")));
        verify(repository).createInstitution("School", "SCHOOL", "ACTIVE", actorId);
    }
    @Test void patchPreservesUnspecifiedFields() {
        UUID id = UUID.randomUUID();
        when(repository.institutionExists(id)).thenReturn(true);
        var expected = new InstitutionResponse(id, "New name", "SCHOOL", "ACTIVE");
        when(repository.findInstitutionById(id)).thenReturn(Optional.of(expected));
        assertEquals(expected, service.updateInstitution(id, new UpdateInstitutionRequest(" New name ", null, null)));
        verify(repository).updateInstitution(id, "New name", null, null);
    }
    @Test void listsUsersOnlyForRequestedInstitution() {
        UUID id = UUID.randomUUID();
        when(repository.institutionExists(id)).thenReturn(true);
        when(repository.findUsers(id)).thenReturn(List.of());
        assertEquals(List.of(), service.getInstitutionUsers(id));
        verify(repository).findUsers(id);
    }
    @Test void duplicateUsernameStopsOfficerCreation() {
        UUID id = UUID.randomUUID();
        when(repository.institutionExists(id)).thenReturn(true);
        when(accounts.existsByUsernameNormalized("staff")).thenReturn(true);
        assertThrows(IllegalArgumentException.class, () -> service.createInstitutionUser(id,
                new CreateInstitutionUserRequest("Staff", "staff", "test-password")));
        verify(repository, never()).createInstitutionUser(any(), any(), any());
        verifyNoInteractions(encoder, audit);
    }
    @Test void missingInstitutionStopsOfficerCreation() {
        assertThrows(IllegalArgumentException.class, () -> service.createInstitutionUser(UUID.randomUUID(),
                new CreateInstitutionUserRequest("Staff", "staff", "test-password")));
        verifyNoInteractions(accounts, encoder, audit);
    }
}
