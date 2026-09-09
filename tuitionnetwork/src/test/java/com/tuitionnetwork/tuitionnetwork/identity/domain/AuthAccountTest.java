package com.tuitionnetwork.tuitionnetwork.identity.domain;

import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.Instant;

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

}