package com.tuitionnetwork.tuitionnetwork.billing.api;

import com.tuitionnetwork.tuitionnetwork.billing.api.dto.StudentFeeRecordResponse;
import com.tuitionnetwork.tuitionnetwork.billing.api.dto.StudentReceiptResponse;
import com.tuitionnetwork.tuitionnetwork.billing.domain.Fee;
import com.tuitionnetwork.tuitionnetwork.billing.service.FeeService;
import com.tuitionnetwork.tuitionnetwork.billing.service.StudentReceiptService;
import com.tuitionnetwork.tuitionnetwork.identity.domain.AuthenticatedActor;
import com.tuitionnetwork.tuitionnetwork.identity.domain.RoleCode;
import com.tuitionnetwork.tuitionnetwork.identity.service.IdentityAccessService;
import com.tuitionnetwork.tuitionnetwork.identity.service.IdentityQueryService;
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
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/v1/billing")
@SecurityRequirement(name="bearerAuth")
public class BillingController {

    private final FeeService feeService;
    private final StudentReceiptService studentReceiptService;
    private final IdentityAccessService identityAccessService;
    private final IdentityQueryService identityQueryService;

    public BillingController(
            FeeService feeService,
            StudentReceiptService studentReceiptService,
            IdentityAccessService identityAccessService,
            IdentityQueryService identityQueryService
    ) {
        this.feeService=feeService;
        this.studentReceiptService=studentReceiptService;
        this.identityAccessService=identityAccessService;
        this.identityQueryService=identityQueryService;
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

        String studentName = identityQueryService.findStudentNames(Set.of(studentId)).get(studentId);
        String institutionName = feeService.findInstitutionNames(Set.of(institutionId)).get(institutionId);

        return StudentFeeRecordResponse.from(
                studentId,
                studentName,
                institutionName,
                fees
        );
    }

    /* institution officer views issued receipts for a student at their school */
    @GetMapping("/students/{studentId}/receipts")
    public List<StudentReceiptResponse> getStudentReceipts(
            @PathVariable UUID studentId
    ) {
        AuthenticatedActor actor=identityAccessService.currentActor();
        UUID institutionId=actor.institutionId();
        identityAccessService.requireInstitutionAccess(institutionId);

        String studentName = identityQueryService.findStudentNames(Set.of(studentId)).get(studentId);
        String institutionName = feeService.findInstitutionNames(Set.of(institutionId)).get(institutionId);

        return studentReceiptService.getStudentReceipts(studentId, institutionId).stream()
                .map(receipt -> receipt.withNames(studentName, institutionName))
                .toList();
    }

    /* institution officer or back-office staff views collections by status */
    @GetMapping("/collections")
    public List<InstitutionCollectionResponse> getInstitutionCollections(
        @RequestParam FeeStatus status,
        @RequestParam (required =false) FeeType feeType,
        @RequestParam (required = false) String collectionPeriod,
        @RequestParam (required = false) UUID institutionId
    ){
        UUID scopedInstitutionId = scopedInstitutionId(institutionId);

        List<Fee> fees=feeService.getInstitutionCollections(
            scopedInstitutionId,
             status,
             feeType,
             collectionPeriod);

        Map<UUID, String> studentNames = identityQueryService.findStudentNames(
                fees.stream().map(Fee::getStudentId).collect(Collectors.toSet()));

             return fees.stream()
             .map(fee -> InstitutionCollectionResponse.from(fee, studentNames.get(fee.getStudentId())))
             .toList();
    }

    private UUID scopedInstitutionId(UUID requestedInstitutionId) {
        AuthenticatedActor actor=identityAccessService.currentActor();
        if (actor.role() == RoleCode.INSTITUTION_OFFICER) {
            UUID institutionId=actor.institutionId();
            identityAccessService.requireInstitutionAccess(institutionId);
            return institutionId;
        }
        identityAccessService.requireBackofficeStaff();
        return requestedInstitutionId;
    }
}
