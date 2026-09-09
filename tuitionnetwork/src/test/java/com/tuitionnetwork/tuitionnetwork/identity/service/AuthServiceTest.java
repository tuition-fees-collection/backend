package com.tuitionnetwork.tuitionnetwork.identity.service;

import com.tuitionnetwork.tuitionnetwork.identity.domain.AuthAccount;
import com.tuitionnetwork.tuitionnetwork.identity.infra.repository.AuthAccountRepository;
import com.tuitionnetwork.tuitionnetwork.identity.service.auth.AuthenticationFailureReason;
import com.tuitionnetwork.tuitionnetwork.identity.service.auth.CredentialAuthenticationResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
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

    @BeforeEach
    void setUp() {
        authAccountRepository =
                Mockito.mock(AuthAccountRepository.class);

        passwordEncoder =
                Mockito.mock(PasswordEncoder.class);

        authService =
                new AuthService(
                        authAccountRepository,
                        passwordEncoder
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

        assertEquals(
                AuthenticationFailureReason.UNKNOWN_ACCOUNT,
                result.failureReason()
        );
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

    assertEquals(
            AuthenticationFailureReason.BAD_PASSWORD,
            result.failureReason()
    );

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

    CredentialAuthenticationResult result =
            authService.authenticateCredentials(
                    "payer01",
                    "correct-password"
            );

    assertTrue(result.authenticated());

    assertNull(result.failureReason());

    assertEquals(
            account.getId(),
            result.accountId()
    );
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

    assertEquals(
            AuthenticationFailureReason.ACCOUNT_INACTIVE,
            result.failureReason()
    );

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

    assertEquals(
            AuthenticationFailureReason.ACCOUNT_LOCKED,
            result.failureReason()
    );

    verifyNoInteractions(passwordEncoder);
    }
    
}