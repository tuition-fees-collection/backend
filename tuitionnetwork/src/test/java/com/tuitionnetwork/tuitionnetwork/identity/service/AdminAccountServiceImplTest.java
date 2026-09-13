package com.tuitionnetwork.tuitionnetwork.identity.service;

import com.tuitionnetwork.tuitionnetwork.identity.domain.*;
import com.tuitionnetwork.tuitionnetwork.identity.infra.repository.AuthAccountRepository;
import org.junit.jupiter.api.Test;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class AdminAccountServiceImplTest {
    private final IdentityAccessService access = mock(IdentityAccessService.class);
    private final AuthAccountRepository repository = mock(AuthAccountRepository.class);
    private final PasswordEncoder encoder = mock(PasswordEncoder.class);
    private final AuditService audit = mock(AuditService.class);
    private final AdminAccountService service = new AdminAccountServiceImpl(access, repository, encoder, audit);

    @Test void nonAdminCannotManageAnyAccount() {
        doThrow(new AccessDeniedException("Access denied")).when(access).requireAdmin();
        UUID id = UUID.randomUUID();
        assertThrows(AccessDeniedException.class, () -> service.createBackofficeAccount("staff", "pw", id));
        assertThrows(AccessDeniedException.class, () -> service.createInstitutionAccount("staff", "pw", id));
        assertThrows(AccessDeniedException.class, () -> service.deactivate(id));
        assertThrows(AccessDeniedException.class, () -> service.reactivate(id));
        verifyNoInteractions(repository, encoder, audit);
    }

    @Test void adminDeactivatesStaffAndIncrementsVersion() {
        UUID id = UUID.randomUUID();
        var account = AuthAccount.forBackofficeEmployee("staff", "hash", UUID.randomUUID());
        when(repository.findById(id)).thenReturn(Optional.of(account));
        service.deactivate(id);
        assertFalse(account.isActive());
        assertEquals(1, account.getTokenVersion());
        verify(audit).recordAuthenticated(eq(AuditAction.STAFF_DEACTIVATED), any());
    }

    @Test void adminCannotDeactivateOrReactivateGuardian() {
        UUID id = UUID.randomUUID();
        var account = AuthAccount.forGuardian("payer", "hash", UUID.randomUUID());
        when(repository.findById(id)).thenReturn(Optional.of(account));
        assertThrows(AccessDeniedException.class, () -> service.deactivate(id));
        assertThrows(AccessDeniedException.class, () -> service.reactivate(id));
        assertTrue(account.isActive());
        assertEquals(0, account.getTokenVersion());
        verify(repository, never()).flush();
        verify(audit, times(2)).recordAuthenticated(eq(AuditAction.AUTHORIZATION_DENIED), any());
    }

    @Test void reactivationPreservesRevocationVersion() {
        UUID id = UUID.randomUUID();
        var account = AuthAccount.forInstitutionUser("staff", "hash", UUID.randomUUID());
        account.deactivate();
        when(repository.findById(id)).thenReturn(Optional.of(account));
        service.reactivate(id);
        assertTrue(account.isActive());
        assertEquals(1, account.getTokenVersion());
        verify(audit).recordAuthenticated(eq(AuditAction.STAFF_REACTIVATED), any());
    }

    @Test void creationNormalizesAndHashesForBothProfileTypes() {
        UUID profileId = UUID.randomUUID();
        UUID accountId = UUID.randomUUID();
        when(encoder.encode("password")).thenReturn("encoded-password");
        when(repository.saveAndFlush(any())).thenAnswer(invocation -> {
            AuthAccount account = invocation.getArgument(0);
            assertEquals("staff", account.getUsernameNormalized());
            assertEquals("encoded-password", account.getPasswordHash());
            assertTrue(account.isStaffAccount());
            ReflectionTestUtils.setField(account, "id", accountId);
            return account;
        });
        assertEquals(accountId, service.createBackofficeAccount(" Staff ", "password", profileId));
        assertEquals(accountId, service.createInstitutionAccount(" Staff ", "password", profileId));
        verify(audit, times(2)).recordAuthenticated(eq(AuditAction.STAFF_CREATED), any());
    }

    @Test void blankCredentialsAreRejected() {
        assertThrows(IllegalArgumentException.class,
                () -> service.createBackofficeAccount("staff", " ", UUID.randomUUID()));
        assertThrows(IllegalArgumentException.class,
                () -> service.createInstitutionAccount(" ", "password", UUID.randomUUID()));
        verifyNoInteractions(repository, encoder, audit);
    }
}
