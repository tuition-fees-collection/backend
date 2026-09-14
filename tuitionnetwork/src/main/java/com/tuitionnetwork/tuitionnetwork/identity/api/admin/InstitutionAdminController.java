package com.tuitionnetwork.tuitionnetwork.identity.api.admin;

import com.tuitionnetwork.tuitionnetwork.identity.service.InstitutionAdminService;
import com.tuitionnetwork.tuitionnetwork.identity.api.AuthenticationErrorResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import java.util.*;

@RestController
@RequestMapping("/api/v1/admin/institutions")
@SecurityRequirement(name = "bearerAuth")
public class InstitutionAdminController {
    private final InstitutionAdminService service;
    public InstitutionAdminController(InstitutionAdminService service) { this.service = service; }
    @GetMapping
    public List<InstitutionResponse> getAll() { return service.getInstitutions(); }
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public InstitutionResponse create(@Valid @RequestBody CreateInstitutionRequest request) {
        return service.createInstitution(request);
    }
    @PatchMapping("/{institutionId}")
    public InstitutionResponse update(@PathVariable UUID institutionId, @RequestBody UpdateInstitutionRequest request) {
        return service.updateInstitution(institutionId, request);
    }
    @GetMapping("/{institutionId}/users")
    public List<InstitutionUserResponse> users(@PathVariable UUID institutionId) {
        return service.getInstitutionUsers(institutionId);
    }
    @PostMapping("/{institutionId}/users")
    @ResponseStatus(HttpStatus.CREATED)
    public InstitutionUserResponse createUser(@PathVariable UUID institutionId,
                                             @Valid @RequestBody CreateInstitutionUserRequest request) {
        return service.createInstitutionUser(institutionId, request);
    }
    @ExceptionHandler(IllegalArgumentException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public AuthenticationErrorResponse invalidRequest() {
        return new AuthenticationErrorResponse("INVALID_INSTITUTION_REQUEST", "Invalid institution request.");
    }
    @ExceptionHandler(org.springframework.dao.DataIntegrityViolationException.class)
    @ResponseStatus(HttpStatus.CONFLICT)
    public AuthenticationErrorResponse conflict() {
        return new AuthenticationErrorResponse("INSTITUTION_CONFLICT", "Institution operation could not be completed.");
    }
}
