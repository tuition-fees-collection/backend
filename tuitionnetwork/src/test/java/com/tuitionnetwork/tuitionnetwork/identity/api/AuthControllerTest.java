package com.tuitionnetwork.tuitionnetwork.identity.api;

import com.tuitionnetwork.tuitionnetwork.identity.domain.*;
import com.tuitionnetwork.tuitionnetwork.identity.service.AuthService;
import com.tuitionnetwork.tuitionnetwork.identity.service.auth.*;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.UUID;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class AuthControllerTest {
    @Test
    void shouldReturnTokenAfterSuccessfulLogin() throws Exception {
        AuthService service = mock(AuthService.class);
        TokenIssuer issuer = mock(TokenIssuer.class);
        var actor = new AuthenticatedActor(UUID.randomUUID(), UUID.randomUUID(),
                ActorType.GUARDIAN, RoleCode.PAYER, null, null);
        when(service.authenticateCredentials("payer", "password"))
                .thenReturn(CredentialAuthenticationResult.success(actor, 3));
        when(issuer.issue(actor, 3)).thenReturn("signed-token");

        MockMvcBuilders.standaloneSetup(new AuthController(service, issuer)).build()
                .perform(post("/api/v1/auth/login").contentType("application/json")
                        .content("{\"username\":\"payer\",\"password\":\"password\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").value("signed-token"))
                .andExpect(jsonPath("$.tokenType").value("Bearer"))
                .andExpect(jsonPath("$.expiresInSeconds").value(900));
        verify(issuer).issue(actor, 3);
    }

    @Test
    void shouldHideEveryFailureReasonAndNeverIssueToken() throws Exception {
        AuthService service = mock(AuthService.class);
        TokenIssuer issuer = mock(TokenIssuer.class);
        var mvc = MockMvcBuilders.standaloneSetup(new AuthController(service, issuer)).build();
        for (AuthenticationFailureReason reason : AuthenticationFailureReason.values()) {
            when(service.authenticateCredentials("payer", "wrong"))
                    .thenReturn(CredentialAuthenticationResult.failure(reason));
            mvc.perform(post("/api/v1/auth/login").contentType("application/json")
                            .content("{\"username\":\"payer\",\"password\":\"wrong\"}"))
                    .andExpect(status().isUnauthorized())
                    .andExpect(jsonPath("$.code").value("AUTHENTICATION_FAILED"))
                    .andExpect(jsonPath("$.message").value("Invalid credentials."));
        }
        verifyNoInteractions(issuer);
    }
}
