package com.tuitionnetwork.tuitionnetwork.identity.service;

import com.tuitionnetwork.tuitionnetwork.identity.api.admin.*;
import com.tuitionnetwork.tuitionnetwork.identity.domain.*;
import com.tuitionnetwork.tuitionnetwork.identity.infra.repository.*;
import org.junit.jupiter.api.Test;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class BackofficeAdminServiceImplTest {
    private final IdentityAccessService access = mock(IdentityAccessService.class);
    private final BackofficeAdminRepository profiles = mock(BackofficeAdminRepository.class);
    private final AuthAccountRepository accounts = mock(AuthAccountRepository.class);
    private final PasswordEncoder encoder = mock(PasswordEncoder.class);
    private final AuditService audit = mock(AuditService.class);
    private final BackofficeAdminService service = new BackofficeAdminServiceImpl(access, profiles, accounts, encoder, audit);

    private CreateBackofficeEmployeeRequest request(RoleCode role) {
        return new CreateBackofficeEmployeeRequest(" Test Staff ", " Branch ", role, " STAFF ", "test-password");
    }

    @Test void adminCanListStaff() {
        when(profiles.findAll()).thenReturn(List.of());
        assertEquals(List.of(), service.getAll());
        var order = inOrder(access, profiles);
        order.verify(access).requireAdmin();
        order.verify(profiles).findAll();
    }

    @Test void nonAdminCannotReadOrWrite() {
        doThrow(new AccessDeniedException("Access denied")).when(access).requireAdmin();
        assertThrows(AccessDeniedException.class, service::getAll);
        assertThrows(AccessDeniedException.class, () -> service.create(request(RoleCode.BANK_OFFICER)));
        assertThrows(AccessDeniedException.class, () -> service.changeRole(UUID.randomUUID(), new ChangeBackofficeRoleRequest(RoleCode.ADMIN)));
        verifyNoInteractions(profiles, accounts, encoder, audit);
    }

    @Test void createsBothApprovedRolesWithEncodedCredentials() {
        for (RoleCode role : List.of(RoleCode.BANK_OFFICER, RoleCode.ADMIN)) {
            UUID employeeId = UUID.randomUUID();
            UUID accountId = UUID.randomUUID();
            UUID roleId = UUID.randomUUID();
            when(profiles.findRoleId(role)).thenReturn(Optional.of(roleId));
            when(profiles.createEmployee("Test Staff", "Branch", roleId)).thenReturn(employeeId);
            when(encoder.encode("test-password")).thenReturn("encoded");
            doAnswer(call -> {
                AuthAccount account = call.getArgument(0);
                assertEquals(employeeId, account.getBackofficeEmployeeId());
                assertEquals("staff", account.getUsernameNormalized());
                assertEquals("encoded", account.getPasswordHash());
                ReflectionTestUtils.setField(account, "id", accountId);
                return account;
            }).when(accounts).saveAndFlush(any());
            var response = service.create(request(role));
            assertEquals(new BackofficeEmployeeResponse(employeeId, "Test Staff", "Branch", role,
                    accountId, "staff", "ACTIVE"), response);
        }
        verify(audit, times(2)).recordAuthenticated(eq(AuditAction.STAFF_CREATED), any());
    }

    @Test void rejectsUnapprovedRoles() {
        for (RoleCode role : List.of(RoleCode.PAYER, RoleCode.INSTITUTION_OFFICER)) {
            assertThrows(IllegalArgumentException.class, () -> service.create(request(role)));
            assertThrows(IllegalArgumentException.class,
                    () -> service.changeRole(UUID.randomUUID(), new ChangeBackofficeRoleRequest(role)));
        }
        verifyNoInteractions(profiles, accounts, encoder, audit);
    }

    @Test void duplicateUsernameIsRejectedBeforeProfileCreation() {
        when(accounts.existsByUsernameNormalized("staff")).thenReturn(true);
        assertThrows(IllegalArgumentException.class, () -> service.create(request(RoleCode.ADMIN)));
        verifyNoInteractions(profiles, encoder, audit);
    }

    @Test void roleChangeInvalidatesEveryAccountForEmployee() {
        UUID employeeId = UUID.randomUUID();
        UUID roleId = UUID.randomUUID();
        var first = AuthAccount.forBackofficeEmployee("one", "hash", employeeId);
        var second = AuthAccount.forBackofficeEmployee("two", "hash", employeeId);
        when(profiles.existsById(employeeId)).thenReturn(true);
        when(profiles.findRoleId(RoleCode.ADMIN)).thenReturn(Optional.of(roleId));
        when(accounts.findAllByBackofficeEmployeeId(employeeId)).thenReturn(List.of(first, second));
        service.changeRole(employeeId, new ChangeBackofficeRoleRequest(RoleCode.ADMIN));
        verify(profiles).updateRole(employeeId, roleId);
        assertEquals(1, first.getTokenVersion());
        assertEquals(1, second.getTokenVersion());
        verify(audit).recordAuthenticated(eq(AuditAction.STAFF_ROLE_CHANGED), any());
    }

    @Test void unknownEmployeeIsRejected() {
        assertThrows(IllegalArgumentException.class,
                () -> service.changeRole(UUID.randomUUID(), new ChangeBackofficeRoleRequest(RoleCode.ADMIN)));
        verifyNoInteractions(accounts, encoder, audit);
    }
}
