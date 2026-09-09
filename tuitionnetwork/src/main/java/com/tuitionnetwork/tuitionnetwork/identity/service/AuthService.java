package com.tuitionnetwork.tuitionnetwork.identity.service;

import com.tuitionnetwork.tuitionnetwork.identity.infra.repository.AuthAccountRepository;
import com.tuitionnetwork.tuitionnetwork.identity.service.auth.CredentialAuthenticationResult;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import java.util.Locale;
import com.tuitionnetwork.tuitionnetwork.identity.domain.AuthAccount;
import com.tuitionnetwork.tuitionnetwork.identity.domain.AuthenticatedActor;
import com.tuitionnetwork.tuitionnetwork.identity.service.auth.AuthenticatedActorResolver;
import com.tuitionnetwork.tuitionnetwork.identity.service.auth.AuthenticationFailureReason;
import org.springframework.transaction.annotation.Transactional;


import java.time.Duration;
import java.time.Instant;
import java.util.Optional;

@Service
public class AuthService {

    private final AuthAccountRepository authAccountRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticatedActorResolver authenticatedActorResolver;

    public AuthService(
                AuthAccountRepository authAccountRepository,
                PasswordEncoder passwordEncoder,
                AuthenticatedActorResolver authenticatedActorResolver
        ) {
        this.authAccountRepository = authAccountRepository;
        this.passwordEncoder = passwordEncoder;
        this.authenticatedActorResolver = authenticatedActorResolver;
        }
        private String normalizeUsername(String username) {

    if (username == null) {
        return "";
    }

    return username
            .trim()
            .toLowerCase(Locale.ROOT);
    }
    @Transactional
public CredentialAuthenticationResult authenticateCredentials(
        String username,
        String rawPassword
) {

    String normalizedUsername =
            normalizeUsername(username);

    Optional<AuthAccount> accountOptional =
            authAccountRepository.findForAuthentication(
                    normalizedUsername
            );

    if (accountOptional.isEmpty()) {
        return CredentialAuthenticationResult.failure(
                AuthenticationFailureReason.UNKNOWN_ACCOUNT
        );
    }

    AuthAccount account = accountOptional.get();

    Instant now = Instant.now();

    if (!account.isActive()) {
        return CredentialAuthenticationResult.failure(
                AuthenticationFailureReason.ACCOUNT_INACTIVE
        );
    }

    if (account.isLocked(now)) {
        return CredentialAuthenticationResult.failure(
                AuthenticationFailureReason.ACCOUNT_LOCKED
        );
    }

    boolean passwordMatches =
            rawPassword != null
            && passwordEncoder.matches(
                    rawPassword,
                    account.getPasswordHash()
            );

    if (!passwordMatches) {

        account.recordFailedLogin(
                now,
                5,
                Duration.ofMinutes(15)
        );

        return CredentialAuthenticationResult.failure(
                AuthenticationFailureReason.BAD_PASSWORD
        );
    }

    account.recordSuccessfulLogin();

        AuthenticatedActor actor =
                authenticatedActorResolver.resolve(account);

        return CredentialAuthenticationResult.success(actor, account.getTokenVersion());
    }
}