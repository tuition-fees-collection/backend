package com.tuitionnetwork.tuitionnetwork.billing.api;

import com.tuitionnetwork.tuitionnetwork.billing.api.dto.StudentFeeRecordResponse;
import com.tuitionnetwork.tuitionnetwork.billing.domain.Fee;
import com.tuitionnetwork.tuitionnetwork.billing.service.FeeService;
import com.tuitionnetwork.tuitionnetwork.identity.domain.AuthenticatedActor;
import com.tuitionnetwork.tuitionnetwork.identity.service.IdentityAccessService;
import com.tuitionnetwork.tuitionnetwork.billing.api.dto.InstitutionCollectionResponse;
import com.tuitionnetwork.tuitionnetwork.billing.domain.FeeStatus;
import com.tuitionnetwork.tuitionnetwork.billing.domain.FeeType;

import io.swagger.v3.oas.annotations.security.SecurityRequirement;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/billing")
@SecurityRequirement(name="bearerAuth")
public class BillingController {

    private final FeeService feeService;
    private final IdentityAccessService identityAccessService;

    public BillingController(
            FeeService feeService,
            IdentityAccessService identityAccessService
    ) {
        this.feeService=feeService;
        this.identityAccessService=identityAccessService;
    }

    /* institution officer views a student's fee record */
    @GetMapping("/students/{studentId}/fees")
    public StudentFeeRecordResponse getStudentFeeRecord(
            @PathVariable UUID studentId
    ) {

        AuthenticatedActor actor=identityAccessService.currentActor();

        UUID institutionId=actor.institutionId();

        identityAccessService.requireInstitutionAccess(
                institutionId
        );

        List<Fee> fees=
                feeService.getFeesForStudentAndInstitution(
                        studentId,
                        institutionId
                );

                

        return StudentFeeRecordResponse.from(
                studentId,
                fees
        );
    }

    /* institution officer views collections by status with optional filters */
    @GetMapping("/collections")
    public List<InstitutionCollectionResponse> getInstitutionCollections(
        @RequestParam FeeStatus status,
        @RequestParam (required =false) FeeType feeType,
        @RequestParam (required = false) String collectionPeriod
    ){
        AuthenticatedActor actor=identityAccessService.currentActor();
        UUID institutionId=actor.institutionId();
        identityAccessService.requireInstitutionAccess(institutionId);

        List<Fee> fees=feeService.getInstitutionCollections(
            institutionId,
             status,
             feeType,
             collectionPeriod);

             return fees.stream()
             .map(InstitutionCollectionResponse::from)
             .toList();
    }
}