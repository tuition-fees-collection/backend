package com.tuitionnetwork.tuitionnetwork.identity.infra.security;

import com.tuitionnetwork.tuitionnetwork.identity.domain.ActorType;
import com.tuitionnetwork.tuitionnetwork.identity.domain.AuthenticatedActor;
import com.tuitionnetwork.tuitionnetwork.identity.domain.RoleCode;
import com.tuitionnetwork.tuitionnetwork.identity.service.CurrentActorProvider;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
public class SpringSecurityCurrentActorProvider implements CurrentActorProvider {
    @Override
    public AuthenticatedActor currentActor() {
        var authentication = SecurityContextHolder.getContext().getAuthentication();
        if (!(authentication instanceof JwtAuthenticationToken jwtAuthentication)
                || !authentication.isAuthenticated()) {
            throw new IllegalStateException("No authenticated actor");
        }
        var jwt = jwtAuthentication.getToken();
        String institutionClaim = jwt.getClaimAsString("institution_id");
        UUID institutionId = institutionClaim == null ? null : UUID.fromString(institutionClaim);
        return new AuthenticatedActor(
                UUID.fromString(jwt.getSubject()),
                UUID.fromString(jwt.getClaimAsString("actor_id")),
                ActorType.valueOf(jwt.getClaimAsString("actor_type")),
                RoleCode.valueOf(jwt.getClaimAsString("role")),
                institutionId,
                jwt.getClaimAsString("branch"));
    }
}
