package com.tuitionnetwork.tuitionnetwork.identity.infra.security;

import com.tuitionnetwork.tuitionnetwork.identity.infra.repository.AuthAccountRepository;
import org.springframework.security.oauth2.core.*;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Component
public class JwtAccountStateValidator implements OAuth2TokenValidator<Jwt> {
    private static final OAuth2Error INVALID_ACCOUNT =
            new OAuth2Error("invalid_token", "Token is no longer valid", null);
    private final AuthAccountRepository repository;

    public JwtAccountStateValidator(AuthAccountRepository repository) {
        this.repository = repository;
    }

    @Override
    public OAuth2TokenValidatorResult validate(Jwt jwt) {
        UUID accountId;
        long version;
        try {
            accountId = UUID.fromString(jwt.getSubject());
            Object claim = jwt.getClaim("token_version");
            if (!(claim instanceof Number number)) {
                return failure();
            }
            // Reject fractions and overflow instead of silently truncating a malformed claim.
            version = new BigDecimal(number.toString()).longValueExact();
            if (version < 0) {
                return failure();
            }
        } catch (IllegalArgumentException | ArithmeticException ex) {
            return failure();
        } catch (NullPointerException ex) {
            return failure();
        }
        var account = repository.findById(accountId).orElse(null);
        if (account == null || !account.isActive() || account.isLocked(Instant.now())
                || account.getTokenVersion() != version) {
            return failure();
        }
        return OAuth2TokenValidatorResult.success();
    }

    private OAuth2TokenValidatorResult failure() {
        return OAuth2TokenValidatorResult.failure(INVALID_ACCOUNT);
    }
}
