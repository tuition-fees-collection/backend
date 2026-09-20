package com.tuitionnetwork.tuitionnetwork.identity.api.admin;

import com.tuitionnetwork.tuitionnetwork.identity.service.BackofficeAdminService;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping(
        "/api/v1/admin/backoffice-employees"
)
@SecurityRequirement(name = "bearerAuth")
public class BackofficeAdminController {

    private final BackofficeAdminService service;

    public BackofficeAdminController(
            BackofficeAdminService service
    ) {
        this.service = service;
    }

    @GetMapping
    public List<BackofficeEmployeeResponse>
    getAll() {

        return service.getAll();
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public BackofficeEmployeeResponse create(
            @Valid
            @RequestBody
            CreateBackofficeEmployeeRequest request
    ) {

        return service.create(request);
    }

    @PatchMapping("/{employeeId}/role")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void changeRole(
            @PathVariable UUID employeeId,
            @Valid
            @RequestBody
            ChangeBackofficeRoleRequest request
    ) {

        service.changeRole(
                employeeId,
                request
        );
    }
}
