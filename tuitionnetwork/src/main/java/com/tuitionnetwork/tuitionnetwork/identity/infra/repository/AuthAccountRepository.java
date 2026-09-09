package com.tuitionnetwork.tuitionnetwork.identity.infra.repository;

import com.tuitionnetwork.tuitionnetwork.identity.domain.AuthAccount;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface AuthAccountRepository
        extends JpaRepository<AuthAccount, UUID> {

    Optional<AuthAccount> findByUsernameNormalized(
            String usernameNormalized
    );
}