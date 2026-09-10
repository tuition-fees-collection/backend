package com.tuitionnetwork.tuitionnetwork.identity.infra.repository;

import com.tuitionnetwork.tuitionnetwork.identity.domain.AuthAccount;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface AuthAccountRepository
        extends JpaRepository<AuthAccount, UUID> {

    boolean existsByUsernameNormalized(String usernameNormalized);

    Optional<AuthAccount> findByBackofficeEmployeeId(UUID backofficeEmployeeId);

    java.util.List<AuthAccount> findAllByBackofficeEmployeeId(UUID backofficeEmployeeId);

    Optional<AuthAccount> findByUsernameNormalized(
            String usernameNormalized
    );
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
        select a
        from AuthAccount a
        where a.usernameNormalized = :username
        """)
    Optional<AuthAccount> findForAuthentication(
            @Param("username") String username
    );
}