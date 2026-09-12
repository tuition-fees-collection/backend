package com.tuitionnetwork.tuitionnetwork.identity.infra.security;

import com.tuitionnetwork.tuitionnetwork.identity.domain.ActorType;
import com.tuitionnetwork.tuitionnetwork.identity.domain.AuthenticatedActor;
import com.tuitionnetwork.tuitionnetwork.identity.domain.RoleCode;
import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;

import java.nio.charset.StandardCharsets;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class SpringJwtTokenIssuerTest {

    @Test
    void shouldIssueAndDecodeValidJwt() {

        SecretKey secretKey =
                new SecretKeySpec(
                        "01234567890123456789012345678901"
                                .getBytes(StandardCharsets.UTF_8),
                        "HmacSHA256"
                );

        var encoder =
                NimbusJwtEncoder
                        .withSecretKey(secretKey)
                        .algorithm(MacAlgorithm.HS256)
                        .build();

        var decoder =
                NimbusJwtDecoder
                        .withSecretKey(secretKey)
                        .macAlgorithm(MacAlgorithm.HS256)
                        .build();

        SpringJwtTokenIssuer issuer =
                new SpringJwtTokenIssuer(encoder);

        UUID accountId = UUID.randomUUID();
        UUID guardianId = UUID.randomUUID();

        AuthenticatedActor actor =
                new AuthenticatedActor(
                        accountId,
                        guardianId,
                        ActorType.GUARDIAN,
                        RoleCode.PAYER,
                        null,
                        null
                );

        String token =
                issuer.issue(actor, 0);

        assertNotNull(token);
        assertFalse(token.isBlank());

        Jwt jwt = decoder.decode(token);

        assertEquals(
                accountId.toString(),
                jwt.getSubject()
        );

        assertEquals(
                guardianId.toString(),
                jwt.getClaimAsString("actor_id")
        );

        assertEquals(
                "GUARDIAN",
                jwt.getClaimAsString("actor_type")
        );

        assertEquals(
                "PAYER",
                jwt.getClaimAsString("role")
        );

        Number tokenVersion = jwt.getClaim("token_version");
        assertNotNull(tokenVersion);
        assertEquals(0L, tokenVersion.longValue());
    }
}