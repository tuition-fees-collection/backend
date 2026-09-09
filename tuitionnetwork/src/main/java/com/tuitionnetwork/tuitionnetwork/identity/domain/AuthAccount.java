package com.tuitionnetwork.tuitionnetwork.identity.domain;

import jakarta.persistence.*;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "auth_account", schema = "identity")
public class AuthAccount {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(
            name = "username_normalized",
            nullable = false,
            unique = true,
            length = 150
    )
    private String usernameNormalized;

    @Column(
            name = "password_hash",
            nullable = false,
            length = 255
    )
    private String passwordHash;

    @Enumerated(EnumType.STRING)
    @Column(
            name = "status",
            nullable = false,
            length = 30
    )
    private AccountStatus status;

    @Column(
            name = "failed_login_attempts",
            nullable = false
    )
    private int failedLoginAttempts;

    @Column(name = "locked_until")
    private Instant lockedUntil;

    @Column(
            name = "token_version",
            nullable = false
    )
    private long tokenVersion;

    @Column(name = "guardian_id")
    private UUID guardianId;

    @Column(name = "backoffice_employee_id")
    private UUID backofficeEmployeeId;

    @Column(name = "institution_user_id")
    private UUID institutionUserId;

    @Column(
            name = "created_at",
            nullable = false
    )
    private Instant createdAt;

    @Column(
            name = "updated_at",
            nullable = false
    )
    private Instant updatedAt;

    @Version
    @Column(name = "version", nullable = false)
    private long version;

    protected AuthAccount() {
    }
}