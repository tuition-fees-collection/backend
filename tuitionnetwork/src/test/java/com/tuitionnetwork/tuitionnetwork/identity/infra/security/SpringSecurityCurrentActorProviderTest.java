package com.tuitionnetwork.tuitionnetwork.identity.infra.security;

import com.tuitionnetwork.tuitionnetwork.identity.domain.*;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class SpringSecurityCurrentActorProviderTest {
    private final SpringSecurityCurrentActorProvider provider = new SpringSecurityCurrentActorProvider();

    @AfterEach
    void clearContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void shouldRejectMissingAuthentication() {
        SecurityContextHolder.clearContext();
        assertThrows(IllegalStateException.class, provider::currentActor);
    }

    @Test
    void shouldReadInstitutionActorFromAuthenticatedJwt() {
        UUID accountId = UUID.randomUUID();
        UUID actorId = UUID.randomUUID();
        UUID institutionId = UUID.randomUUID();
        Jwt jwt = Jwt.withTokenValue("test-token").header("alg", "HS256")
                .subject(accountId.toString())
                .claim("actor_id", actorId.toString())
                .claim("actor_type", "INSTITUTION_USER")
                .claim("role", "INSTITUTION_OFFICER")
                .claim("institution_id", institutionId.toString()).build();
        SecurityContextHolder.getContext().setAuthentication(new JwtAuthenticationToken(jwt, List.of()));

        assertEquals(new AuthenticatedActor(accountId, actorId, ActorType.INSTITUTION_USER,
                RoleCode.INSTITUTION_OFFICER, institutionId, null), provider.currentActor());
    }
}
