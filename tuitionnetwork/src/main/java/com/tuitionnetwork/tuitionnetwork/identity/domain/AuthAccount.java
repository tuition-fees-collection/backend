package com.tuitionnetwork.tuitionnetwork.identity.domain;

import jakarta.persistence.*;

import java.time.Instant;
import java.time.Duration;
import java.util.Objects;
import java.util.UUID;

@Entity
@Table(name = "auth_account", schema = "identity")
public class AuthAccount {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "username_normalized", nullable = false, unique = true, length = 150)
    private String usernameNormalized;

    @Column(name = "password_hash", nullable = false, length = 255)
    private String passwordHash;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 30)
    private AccountStatus status;

    @Column(name = "failed_login_attempts", nullable = false)
    private int failedLoginAttempts;

    @Column(name = "locked_until")
    private Instant lockedUntil;

    @Column(name = "token_version", nullable = false)
    private long tokenVersion;

    @Column(name = "guardian_id")
    private UUID guardianId;

    @Column(name = "backoffice_employee_id")
    private UUID backofficeEmployeeId;

    @Column(name = "institution_user_id")
    private UUID institutionUserId;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Version
    @Column(name = "version", nullable = false)
    private long version;

    protected AuthAccount() {
    }

    private AuthAccount(
            String usernameNormalized,
            String passwordHash
    ) {
        if (usernameNormalized == null || usernameNormalized.isBlank()) {
            throw new IllegalArgumentException(
                    "usernameNormalized must not be blank"
            );
        }

        if (passwordHash == null || passwordHash.isBlank()) {
            throw new IllegalArgumentException(
                    "passwordHash must not be blank"
            );
        }

        this.usernameNormalized = usernameNormalized;
        this.passwordHash = passwordHash;
        this.status = AccountStatus.ACTIVE;
    }

    public static AuthAccount forGuardian(
            String usernameNormalized,
            String passwordHash,
            UUID guardianId
    ) {
        AuthAccount account =
                new AuthAccount(
                        usernameNormalized,
                        passwordHash
                );

        account.guardianId =
                Objects.requireNonNull(
                        guardianId,
                        "guardianId must not be null"
                );

        return account;
    }

    public static AuthAccount forBackofficeEmployee(
            String usernameNormalized,
            String passwordHash,
            UUID backofficeEmployeeId
    ) {
        AuthAccount account =
                new AuthAccount(
                        usernameNormalized,
                        passwordHash
                );

        account.backofficeEmployeeId =
                Objects.requireNonNull(
                        backofficeEmployeeId,
                        "backofficeEmployeeId must not be null"
                );

        return account;
    }

    public static AuthAccount forInstitutionUser(
            String usernameNormalized,
            String passwordHash,
            UUID institutionUserId
    ) {
        AuthAccount account =
                new AuthAccount(
                        usernameNormalized,
                        passwordHash
                );

        account.institutionUserId =
                Objects.requireNonNull(
                        institutionUserId,
                        "institutionUserId must not be null"
                );

        return account;
    }

    public boolean isActive() {
        return status == AccountStatus.ACTIVE;
    }

    public boolean isLocked(Instant now) {
        return lockedUntil != null
                && lockedUntil.isAfter(now);
    }

    public void recordFailedLogin(
            Instant now,
            int maxFailedAttempts,
            Duration lockDuration) {
        resetExpiredLockIfNeeded(now);

        failedLoginAttempts++;

        if (failedLoginAttempts >= maxFailedAttempts) {
            lockedUntil = now.plus(lockDuration);
        }
    }

    public void recordSuccessfulLogin() {
        failedLoginAttempts = 0;
        lockedUntil = null;
    }

    public void activate() {
        status = AccountStatus.ACTIVE;
    }

    public void deactivate() {
        status = AccountStatus.INACTIVE;
        tokenVersion++;
    }

    private void resetExpiredLockIfNeeded(Instant now) {
        if (lockedUntil != null
                && !lockedUntil.isAfter(now)) {

            failedLoginAttempts = 0;
            lockedUntil = null;
        }
    }

    @PrePersist
    void onCreate() {
        Instant now = Instant.now();
        createdAt = now;
        updatedAt = now;
    }

    @PreUpdate
    void onUpdate() {
        updatedAt = Instant.now();
    }

    public UUID getId() {
        return id;
    }

    public String getUsernameNormalized() {
        return usernameNormalized;
    }

    public String getPasswordHash() {
        return passwordHash;
    }

    public AccountStatus getStatus() {
        return status;
    }

    public int getFailedLoginAttempts() {
        return failedLoginAttempts;
    }

    public Instant getLockedUntil() {
        return lockedUntil;
    }

    public long getTokenVersion() {
        return tokenVersion;
    }

    public UUID getGuardianId() {
        return guardianId;
    }

    public UUID getBackofficeEmployeeId() {
        return backofficeEmployeeId;
    }

    public UUID getInstitutionUserId() {
        return institutionUserId;
    }

}