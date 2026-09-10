package com.tuitionnetwork.tuitionnetwork.identity.infra.security;

import com.tuitionnetwork.tuitionnetwork.identity.domain.AuthAccount;
import com.tuitionnetwork.tuitionnetwork.identity.infra.repository.AuthAccountRepository;
import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.jwt.Jwt;
import java.time.*;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class JwtAccountStateValidatorTest {
    private final AuthAccountRepository repository = mock(AuthAccountRepository.class);
    private final JwtAccountStateValidator validator = new JwtAccountStateValidator(repository);
    private final UUID id = UUID.randomUUID();

    private AuthAccount account() {
        var account = AuthAccount.forBackofficeEmployee("staff", "hash", UUID.randomUUID());
        when(repository.findById(id)).thenReturn(Optional.of(account));
        return account;
    }

    private Jwt token(String subject, Object version) {
        var builder = Jwt.withTokenValue("test").header("alg", "HS256").subject(subject);
        if (version != null) builder.claim("token_version", version);
        return builder.build();
    }

    @Test void matchingVersionSucceeds() {
        account();
        assertFalse(validator.validate(token(id.toString(), 0)).hasErrors());
    }
    @Test void missingAccountFails() {
        when(repository.findById(id)).thenReturn(Optional.empty());
        assertTrue(validator.validate(token(id.toString(), 0)).hasErrors());
    }
    @Test void inactiveAccountFails() {
        account().deactivate();
        assertTrue(validator.validate(token(id.toString(), 1)).hasErrors());
    }
    @Test void lockedAccountFails() {
        account().recordFailedLogin(Instant.now(), 1, Duration.ofMinutes(15));
        assertTrue(validator.validate(token(id.toString(), 0)).hasErrors());
    }
    @Test void wrongVersionFails() {
        account();
        assertTrue(validator.validate(token(id.toString(), 1)).hasErrors());
    }
    @Test void missingVersionFailsBeforeLookup() {
        assertTrue(validator.validate(token(id.toString(), null)).hasErrors());
        verifyNoInteractions(repository);
    }
    @Test void invalidSubjectFailsBeforeLookup() {
        assertTrue(validator.validate(token("invalid", 0)).hasErrors());
        verifyNoInteractions(repository);
    }
    @Test void malformedVersionsFailBeforeLookup() {
        for (Object version : List.of("0", 0.5, -1, new java.math.BigInteger("9223372036854775808"))) {
            assertTrue(validator.validate(token(id.toString(), version)).hasErrors());
        }
        verifyNoInteractions(repository);
    }
    @Test void reactivationDoesNotRestoreOldTokens() {
        var account = account();
        account.deactivate();
        account.activate();
        assertTrue(validator.validate(token(id.toString(), 0)).hasErrors());
        assertFalse(validator.validate(token(id.toString(), 1)).hasErrors());
    }
}
