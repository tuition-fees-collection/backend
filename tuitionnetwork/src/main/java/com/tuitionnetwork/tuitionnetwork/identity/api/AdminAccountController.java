package com.tuitionnetwork.tuitionnetwork.identity.api;

import com.tuitionnetwork.tuitionnetwork.identity.service.AdminAccountService;
import org.springframework.dao.DataIntegrityViolationException;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/admin/accounts")
@SecurityRequirement(name = "bearerAuth")
public class AdminAccountController {
    private final AdminAccountService service;

    public AdminAccountController(AdminAccountService service) {
        this.service = service;
    }

    public record BackofficeRequest(String username, String password, UUID backofficeEmployeeId) {}
    public record InstitutionRequest(String username, String password, UUID institutionUserId) {}
    public record CreatedAccount(UUID accountId) {}

    @PostMapping("/backoffice")
    public ResponseEntity<CreatedAccount> createBackoffice(@RequestBody BackofficeRequest request) {
        return ResponseEntity.status(201).body(new CreatedAccount(service.createBackofficeAccount(
                request.username(), request.password(), request.backofficeEmployeeId())));
    }

    @PostMapping("/institution")
    public ResponseEntity<CreatedAccount> createInstitution(@RequestBody InstitutionRequest request) {
        return ResponseEntity.status(201).body(new CreatedAccount(service.createInstitutionAccount(
                request.username(), request.password(), request.institutionUserId())));
    }

    @PatchMapping("/{id}/deactivate")
    public ResponseEntity<Void> deactivate(@PathVariable UUID id) {
        service.deactivate(id);
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/{id}/reactivate")
    public ResponseEntity<Void> reactivate(@PathVariable UUID id) {
        service.reactivate(id);
        return ResponseEntity.noContent().build();
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<AuthenticationErrorResponse> invalidRequest() {
        return ResponseEntity.badRequest().body(new AuthenticationErrorResponse(
                "INVALID_ACCOUNT_REQUEST", "Invalid account request."));
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<AuthenticationErrorResponse> accountConflict() {
        return ResponseEntity.status(409).body(new AuthenticationErrorResponse(
                "ACCOUNT_CONFLICT", "Account could not be created or updated."));
    }
}
