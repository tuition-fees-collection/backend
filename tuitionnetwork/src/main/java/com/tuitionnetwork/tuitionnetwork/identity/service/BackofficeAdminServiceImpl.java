package com.tuitionnetwork.tuitionnetwork.identity.service;

import com.tuitionnetwork.tuitionnetwork.identity.api.admin.BackofficeEmployeeResponse;
import com.tuitionnetwork.tuitionnetwork.identity.api.admin.ChangeBackofficeRoleRequest;
import com.tuitionnetwork.tuitionnetwork.identity.api.admin.CreateBackofficeEmployeeRequest;
import com.tuitionnetwork.tuitionnetwork.identity.domain.AuditAction;
import com.tuitionnetwork.tuitionnetwork.identity.domain.AuthAccount;
import com.tuitionnetwork.tuitionnetwork.identity.domain.AuthenticatedActor;
import com.tuitionnetwork.tuitionnetwork.identity.domain.RoleCode;
import com.tuitionnetwork.tuitionnetwork.identity.infra.repository.AuthAccountRepository;
import com.tuitionnetwork.tuitionnetwork.identity.infra.repository.BackofficeAdminRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Locale;
import java.util.UUID;

@Service
public class BackofficeAdminServiceImpl
        implements BackofficeAdminService {

    private final IdentityAccessService identityAccessService;
    private final BackofficeAdminRepository backofficeRepository;
    private final AuthAccountRepository authAccountRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuditService auditService;

    public BackofficeAdminServiceImpl(
            IdentityAccessService identityAccessService,
            BackofficeAdminRepository backofficeRepository,
            AuthAccountRepository authAccountRepository,
            PasswordEncoder passwordEncoder,
            AuditService auditService
    ) {
        this.identityAccessService =
                identityAccessService;

        this.backofficeRepository =
                backofficeRepository;

        this.authAccountRepository =
                authAccountRepository;

        this.passwordEncoder =
                passwordEncoder;

        this.auditService =
                auditService;
    }

    @Override
    @Transactional(readOnly = true)
    public List<BackofficeEmployeeResponse>
    getAll() {

        identityAccessService.requireAdmin();

        return backofficeRepository.findAll();
    }

    @Override
    @Transactional
    public BackofficeEmployeeResponse create(
            CreateBackofficeEmployeeRequest request
    ) {

        identityAccessService.requireAdmin();

        AuthenticatedActor admin =
                identityAccessService.currentActor();

        if (request == null) throw new IllegalArgumentException("Request is required");
        validateRole(request.role());

        if (request.fullName() == null || request.fullName().isBlank() || request.fullName().trim().length() > 255
                || request.branch() == null || request.branch().isBlank() || request.branch().trim().length() > 100
                || request.password() == null || request.password().isBlank()) {
            throw new IllegalArgumentException("Valid name, branch and password are required");
        }

        String username =
                normalizeUsername(
                        request.username()
                );

        if (authAccountRepository
                .existsByUsernameNormalized(username)) {

            throw new IllegalArgumentException(
                    "Username already exists"
            );
        }

        UUID roleId =
                backofficeRepository
                        .findRoleId(request.role())
                        .orElseThrow(
                                () ->
                                        new IllegalStateException(
                                                "Configured role not found"
                                        )
                        );

        UUID employeeId =
                backofficeRepository
                        .createEmployee(
                                request.fullName().trim(),
                                request.branch().trim(),
                                roleId
                        );

        String passwordHash =
                passwordEncoder.encode(
                        request.password()
                );

        AuthAccount account =
                AuthAccount.forBackofficeEmployee(
                        username,
                        passwordHash,
                        employeeId
                );

        authAccountRepository.saveAndFlush(account);

        auditService.recordAuthenticated(
                AuditAction.STAFF_CREATED,
                admin
        );

        return new BackofficeEmployeeResponse(employeeId, request.fullName().trim(),
                request.branch().trim(), request.role(), account.getId(), username, account.getStatus().name());
    }

    @Override
    @Transactional
    public void changeRole(
            UUID employeeId,
            ChangeBackofficeRoleRequest request
    ) {

        identityAccessService.requireAdmin();

        AuthenticatedActor admin =
                identityAccessService.currentActor();

        if (request == null) throw new IllegalArgumentException("Request is required");
        validateRole(request.role());

        if (!backofficeRepository
                .existsById(employeeId)) {

            throw new IllegalArgumentException(
                    "Back-office employee not found"
            );
        }

        UUID roleId =
                backofficeRepository
                        .findRoleId(request.role())
                        .orElseThrow(
                                () ->
                                        new IllegalStateException(
                                                "Configured role not found"
                                        )
                        );

        backofficeRepository.updateRole(
                employeeId,
                roleId
        );

        authAccountRepository
                .findAllByBackofficeEmployeeId(
                        employeeId
                )
                .forEach(
                        AuthAccount::invalidateTokens
                );

        authAccountRepository.flush();

        auditService.recordAuthenticated(
                AuditAction.STAFF_ROLE_CHANGED,
                admin
        );
    }

    private void validateRole(
            RoleCode role
    ) {

        if (role != RoleCode.ADMIN
                && role != RoleCode.BANK_OFFICER) {

            throw new IllegalArgumentException(
                    "Role is not approved for back-office staff"
            );
        }
    }

    private String normalizeUsername(
            String username
    ) {

        if (username == null) {
            throw new IllegalArgumentException(
                    "Username is required"
            );
        }

        String normalized =
                username.trim()
                        .toLowerCase(
                                Locale.ROOT
                        );

        if (normalized.isBlank() || normalized.length() > 150) {
            throw new IllegalArgumentException(
                    "Username is required"
            );
        }

        return normalized;
    }
}
