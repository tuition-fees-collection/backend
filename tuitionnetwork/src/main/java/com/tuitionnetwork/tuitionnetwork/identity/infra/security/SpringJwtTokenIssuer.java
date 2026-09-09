package com.tuitionnetwork.tuitionnetwork.identity.infra.security;

import com.tuitionnetwork.tuitionnetwork.identity.domain.AuthenticatedActor;
import com.tuitionnetwork.tuitionnetwork.identity.service.auth.TokenIssuer;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Component
public class SpringJwtTokenIssuer implements TokenIssuer {

    private static final Duration ACCESS_TOKEN_LIFETIME = Duration.ofMinutes(15);

    private final JwtEncoder jwtEncoder;

    public SpringJwtTokenIssuer(JwtEncoder jwtEncoder) {
        this.jwtEncoder = jwtEncoder;
    }

    @Override
    public String issue(AuthenticatedActor actor, long tokenVersion) {
        Instant now = Instant.now();

        JwtClaimsSet.Builder claims = JwtClaimsSet.builder()
                .issuer("tuitionnetwork")
                .subject(actor.accountId().toString())
                .audience(List.of("tuitionnetwork-api"))
                .issuedAt(now)
                .expiresAt(now.plus(ACCESS_TOKEN_LIFETIME))
                .id(UUID.randomUUID().toString())
                .claim("actor_id", actor.actorId().toString())
                .claim("actor_type", actor.actorType().name())
                .claim("role", actor.role().name())
                .claim("token_version", tokenVersion);

        if (actor.institutionId() != null) {
            claims.claim("institution_id", actor.institutionId().toString());
        }

        if (actor.branch() != null) {
            claims.claim("branch", actor.branch());
        }

        return jwtEncoder
                .encode(JwtEncoderParameters.from(claims.build()))
                .getTokenValue();
    }
}
