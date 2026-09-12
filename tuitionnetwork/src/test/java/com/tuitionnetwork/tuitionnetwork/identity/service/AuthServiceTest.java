package com.tuitionnetwork.tuitionnetwork.identity.service;

import com.tuitionnetwork.tuitionnetwork.identity.domain.AuthAccount;
import com.tuitionnetwork.tuitionnetwork.identity.domain.AuthenticatedActor;
import com.tuitionnetwork.tuitionnetwork.identity.domain.ActorType;
import com.tuitionnetwork.tuitionnetwork.identity.domain.RoleCode;
import com.tuitionnetwork.tuitionnetwork.identity.infra.repository.AuthAccountRepository;
import com.tuitionnetwork.tuitionnetwork.identity.service.auth.AuthenticatedActorResolver;
import com.tuitionnetwork.tuitionnetwork.identity.service.auth.AuthenticationFailureReason;
import com.tuitionnetwork.tuitionnetwork.identity.service.auth.CredentialAuthenticationResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import com.tuitionnetwork.tuitionnetwork.identity.domain.AuditAction;
import org.mockito.Mockito;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class AuthServiceTest {

    private AuthAccountRepository authAccountRepository;
    private PasswordEncoder passwordEncoder;
    private AuthService authService;
    private AuthenticatedActorResolver authenticatedActorResolver;

    private AuditService auditService;

    @BeforeEach
    void setUp() {
        auditService = mock(AuditService.class);
        authAccountRepository =
                Mockito.mock(AuthAccountRepository.class);

        passwordEncoder =
                Mockito.mock(PasswordEncoder.class);
        authenticatedActorResolver =
                Mockito.mock(
                        AuthenticatedActorResolver.class
                );
        authService =
                new AuthService(
                        authAccountRepository,
                        passwordEncoder,
                        authenticatedActorResolver,
                        auditService
                );
    }
    @Test
    void shouldNormalizeUsernameBeforeLookup() {

        when(
            authAccountRepository
                .findForAuthentication("officer01")
        ).thenReturn(Optional.empty());

        authService.authenticateCredentials(
                "  Officer01  ",
                "password"
        );

        verify(authAccountRepository)
                .findForAuthentication("officer01");
    }
    @Test
    void shouldFailWhenAccountDoesNotExist() {

        when(
            authAccountRepository
                .findForAuthentication("missing")
        ).thenReturn(Optional.empty());

        CredentialAuthenticationResult result =
                authService.authenticateCredentials(
                        "missing",
                        "password"
                );

        assertFalse(result.authenticated());
        assertNull(result.actor());
    assertNull(result.tokenVersion());
        verifyNoInteractions(authenticatedActorResolver);

        assertEquals(
                AuthenticationFailureReason.UNKNOWN_ACCOUNT,
                result.failureReason()
        );
    verify(auditService).recordUnauthenticated(AuditAction.LOGIN_FAILURE_UNKNOWN_ACCOUNT);
    }
    @Test
    void shouldRecordFailureWhenPasswordIsWrong() {

    AuthAccount account =
            AuthAccount.forGuardian(
                    "payer01",
                    "{bcrypt}hash",
                    UUID.randomUUID()
            );

    when(
        authAccountRepository
            .findForAuthentication("payer01")
    ).thenReturn(Optional.of(account));

    when(
        passwordEncoder.matches(
            "wrong-password",
            account.getPasswordHash()
        )
    ).thenReturn(false);

    CredentialAuthenticationResult result =
            authService.authenticateCredentials(
                    "payer01",
                    "wrong-password"
            );

    assertFalse(result.authenticated());
    assertNull(result.actor());
    assertNull(result.tokenVersion());
    verifyNoInteractions(authenticatedActorResolver);

    assertEquals(
            AuthenticationFailureReason.BAD_PASSWORD,
            result.failureReason()
    );
    verify(auditService).recordUnauthenticated(AuditAction.LOGIN_FAILURE_BAD_PASSWORD);

    assertEquals(
            1,
            account.getFailedLoginAttempts()
    );
    }
    @Test
    void shouldAuthenticateWhenPasswordMatches() {

    AuthAccount account =
            AuthAccount.forGuardian(
                    "payer01",
                    "{bcrypt}hash",
                    UUID.randomUUID()
            );

    when(
        authAccountRepository
            .findForAuthentication("payer01")
    ).thenReturn(Optional.of(account));

    when(
        passwordEncoder.matches(
            "correct-password",
            account.getPasswordHash()
        )
    ).thenReturn(true);

    AuthenticatedActor actor = new AuthenticatedActor(
            UUID.randomUUID(),
            account.getGuardianId(),
            ActorType.GUARDIAN,
            RoleCode.PAYER,
            null,
            null
    );

    when(authenticatedActorResolver.resolve(account)).thenReturn(actor);

    CredentialAuthenticationResult result =
            authService.authenticateCredentials(
                    "payer01",
                    "correct-password"
            );

    assertTrue(result.authenticated());
    assertEquals(Long.valueOf(account.getTokenVersion()), result.tokenVersion());

    assertNull(result.failureReason());

    assertEquals(
            actor,
            result.actor()
    );

    verify(authenticatedActorResolver).resolve(account);
    verify(auditService).recordAuthenticated(AuditAction.LOGIN_SUCCESS, actor);
    }
    @Test
    void shouldRejectInactiveAccount() {

    AuthAccount account =
            AuthAccount.forGuardian(
                    "payer01",
                    "{bcrypt}hash",
                    UUID.randomUUID()
            );

    account.deactivate();

    when(
        authAccountRepository
            .findForAuthentication("payer01")
    ).thenReturn(Optional.of(account));

    CredentialAuthenticationResult result =
            authService.authenticateCredentials(
                    "payer01",
                    "password"
            );

    assertFalse(result.authenticated());
    assertNull(result.actor());
    assertNull(result.tokenVersion());
    verifyNoInteractions(authenticatedActorResolver);

    assertEquals(
            AuthenticationFailureReason.ACCOUNT_INACTIVE,
            result.failureReason()
    );
    verify(auditService).recordUnauthenticated(AuditAction.LOGIN_FAILURE_ACCOUNT_INACTIVE);

    verifyNoInteractions(passwordEncoder);
    }
    @Test
    void shouldRejectLockedAccount() {

    AuthAccount account =
            AuthAccount.forGuardian(
                    "payer01",
                    "{bcrypt}hash",
                    UUID.randomUUID()
            );

    var now = java.time.Instant.now();

    for (int i = 0; i < 5; i++) {
        account.recordFailedLogin(
                now,
                5,
                java.time.Duration.ofMinutes(15)
        );
    }

    when(
        authAccountRepository
            .findForAuthentication("payer01")
    ).thenReturn(Optional.of(account));

    CredentialAuthenticationResult result =
            authService.authenticateCredentials(
                    "payer01",
                    "password"
            );

    assertFalse(result.authenticated());
    assertNull(result.actor());
    assertNull(result.tokenVersion());
    verifyNoInteractions(authenticatedActorResolver);

    assertEquals(
            AuthenticationFailureReason.ACCOUNT_LOCKED,
            result.failureReason()
    );
    verify(auditService).recordUnauthenticated(AuditAction.LOGIN_FAILURE_ACCOUNT_LOCKED);

    verifyNoInteractions(passwordEncoder);
    }
    

    @Test
    void fifthBadPasswordAuditsLockTransition() {
        AuthAccount account = AuthAccount.forGuardian("payer01", "{bcrypt}hash", UUID.randomUUID());
        var now = java.time.Instant.now();
        for (int i = 0; i < 4; i++) {
            account.recordFailedLogin(now, 5, java.time.Duration.ofMinutes(15));
        }
        when(authAccountRepository.findForAuthentication("payer01")).thenReturn(Optional.of(account));
        when(passwordEncoder.matches("wrong", account.getPasswordHash())).thenReturn(false);

        var result = authService.authenticateCredentials("payer01", "wrong");

        assertFalse(result.authenticated());
        verify(auditService).recordUnauthenticated(AuditAction.LOGIN_FAILURE_BAD_PASSWORD);
        verify(auditService).recordUnauthenticated(AuditAction.ACCOUNT_LOCKED);
        verifyNoMoreInteractions(auditService);
        verifyNoInteractions(authenticatedActorResolver);
    }
}