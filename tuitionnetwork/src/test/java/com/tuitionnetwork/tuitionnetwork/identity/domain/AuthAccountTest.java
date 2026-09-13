package com.tuitionnetwork.tuitionnetwork.identity.domain;

import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class AuthAccountTest {
    @Test
    void shouldLockAccountAfterFiveFailedAttempts() {
    AuthAccount account = new AuthAccount();

    Instant now =
            Instant.parse("2026-09-09T10:00:00Z");

    for (int i = 0; i < 5; i++) {
        account.recordFailedLogin(
                now,
                5,
                Duration.ofMinutes(15)
        );
    }

    assertEquals(
            5,
            account.getFailedLoginAttempts()
    );

    assertTrue(account.isLocked(now));
    }
    @Test
    void shouldResetFailuresAfterSuccessfulLogin() {
    AuthAccount account = new AuthAccount();

    Instant now =
            Instant.parse("2026-09-09T10:00:00Z");

    account.recordFailedLogin(
            now,
            5,
            Duration.ofMinutes(15)
    );

    account.recordFailedLogin(
            now,
            5,
            Duration.ofMinutes(15)
    );

    account.recordSuccessfulLogin();

    assertEquals(
            0,
            account.getFailedLoginAttempts()
    );

    assertNull(account.getLockedUntil());
    }
    @Test
    void shouldNotRemainLockedAfterLockDurationExpires() {
    AuthAccount account = new AuthAccount();

    Instant now =
            Instant.parse("2026-09-09T10:00:00Z");

    for (int i = 0; i < 5; i++) {
        account.recordFailedLogin(
                now,
                5,
                Duration.ofMinutes(15)
        );
    }

    Instant later =
            now.plus(Duration.ofMinutes(16));

    assertFalse(account.isLocked(later));
    }
    @Test
    void shouldCreateGuardianAccountInValidInitialState() {

        UUID guardianId = UUID.randomUUID();

        AuthAccount account =
                AuthAccount.forGuardian(
                        "payer01",
                        "{bcrypt}test-hash",
                        guardianId
                );

        assertEquals(
                "payer01",
                account.getUsernameNormalized()
        );

        assertEquals(
                AccountStatus.ACTIVE,
                account.getStatus()
        );

        assertEquals(
                guardianId,
                account.getGuardianId()
        );

        assertNull(
                account.getBackofficeEmployeeId()
        );

        assertNull(
                account.getInstitutionUserId()
        );

        assertEquals(
                0,
                account.getFailedLoginAttempts()
        );

        assertEquals(
                0,
                account.getTokenVersion()
        );
    }

    @Test
    void shouldRejectGuardianAccountWithoutGuardianId() {
        assertThrows(NullPointerException.class,
                () -> AuthAccount.forGuardian("payer01", "{bcrypt}hash", null));
    }

    @Test
    void shouldRejectBackofficeAccountWithoutEmployeeId() {
        assertThrows(NullPointerException.class,
                () -> AuthAccount.forBackofficeEmployee("employee01", "{bcrypt}hash", null));
    }

    @Test
    void shouldRejectInstitutionAccountWithoutInstitutionUserId() {
        assertThrows(NullPointerException.class,
                () -> AuthAccount.forInstitutionUser("user01", "{bcrypt}hash", null));
    }

    @Test
    void shouldRejectBlankUsername() {
        assertThrows(IllegalArgumentException.class,
                () -> AuthAccount.forGuardian(" ", "{bcrypt}hash", UUID.randomUUID()));
    }

    @Test
    void shouldRejectBlankPasswordHash() {
        assertThrows(IllegalArgumentException.class,
                () -> AuthAccount.forGuardian("payer01", "", UUID.randomUUID()));
    }

    @Test
    void shouldNotLockAccountAfterFourFailedAttempts() {
        AuthAccount account = AuthAccount.forGuardian("payer01", "{bcrypt}hash", UUID.randomUUID());
        Instant now = Instant.parse("2026-09-09T10:00:00Z");
        Duration lockDuration = Duration.ofMinutes(15);

        for (int i = 0; i < 4; i++) {
            account.recordFailedLogin(now, 5, lockDuration);
        }

        assertEquals(4, account.getFailedLoginAttempts());
        assertFalse(account.isLocked(now));
        assertNull(account.getLockedUntil());
    }

    @Test
    void shouldUnlockAccountExactlyAtExpiry() {
        AuthAccount account = AuthAccount.forGuardian("payer01", "{bcrypt}hash", UUID.randomUUID());
        Instant now = Instant.parse("2026-09-09T10:00:00Z");
        Duration lockDuration = Duration.ofMinutes(15);

        for (int i = 0; i < 5; i++) {
            account.recordFailedLogin(now, 5, lockDuration);
        }

        Instant expiry = now.plus(lockDuration);
        assertEquals(expiry, account.getLockedUntil());
        assertTrue(account.isLocked(expiry.minusNanos(1)));
        assertFalse(account.isLocked(expiry));
    }

    @Test
    void shouldCountFirstFailedAttemptAfterExpiryAsOne() {
        AuthAccount account = AuthAccount.forGuardian("payer01", "{bcrypt}hash", UUID.randomUUID());
        Instant now = Instant.parse("2026-09-09T10:00:00Z");
        Duration lockDuration = Duration.ofMinutes(15);

        for (int i = 0; i < 5; i++) {
            account.recordFailedLogin(now, 5, lockDuration);
        }

        assertTrue(account.isLocked(now));
        Instant expiry = now.plus(lockDuration);
        account.recordFailedLogin(expiry, 5, lockDuration);

        assertEquals(1, account.getFailedLoginAttempts());
        assertNull(account.getLockedUntil());
        assertFalse(account.isLocked(expiry));
    }

    @Test
    void shouldDeactivateAccountAndIncrementTokenVersion() {
        AuthAccount account = AuthAccount.forGuardian("payer01", "{bcrypt}hash", UUID.randomUUID());
        long previousTokenVersion = account.getTokenVersion();
        assertTrue(account.isActive());

        account.deactivate();

        assertEquals(AccountStatus.INACTIVE, account.getStatus());
        assertFalse(account.isActive());
        assertEquals(previousTokenVersion + 1, account.getTokenVersion());
    }
}
