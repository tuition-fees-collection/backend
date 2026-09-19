package com.tuitionnetwork.tuitionnetwork.billing.api;
import com.tuitionnetwork.tuitionnetwork.billing.domain.Fee;
import com.tuitionnetwork.tuitionnetwork.billing.api.dto.FeeSearchRequest;
import com.tuitionnetwork.tuitionnetwork.billing.service.FeeService;
import com.tuitionnetwork.tuitionnetwork.billing.api.dto.CategorizedFeesResponse;
import com.tuitionnetwork.tuitionnetwork.billing.api.dto.StudentFeeRecordResponse;


import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.Set;
import java.util.ArrayList;
import java.util.stream.Collectors;

import jakarta.validation.Valid;

import com.tuitionnetwork.tuitionnetwork.identity.domain.AuthenticatedActor;
import com.tuitionnetwork.tuitionnetwork.identity.domain.NationalId;
import com.tuitionnetwork.tuitionnetwork.identity.domain.RoleCode;
import com.tuitionnetwork.tuitionnetwork.identity.service.GuardianSearchScope;
import com.tuitionnetwork.tuitionnetwork.identity.service.IdentityAccessService;
import com.tuitionnetwork.tuitionnetwork.identity.service.IdentityQueryService;
import com.tuitionnetwork.tuitionnetwork.identity.service.GuardianNotFoundException;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
@RestController
@RequestMapping("/api/v1/fee-searches")
@SecurityRequirement(name="bearerAuth")
public class FeeSearchController{
    private final IdentityAccessService identityAccessService;
    private final IdentityQueryService identityQueryService;
    private final FeeService feeService;

    public FeeSearchController(
            IdentityAccessService identityAccessService,
            IdentityQueryService identityQueryService,
            FeeService feeService
    ) {
        this.identityAccessService=identityAccessService;
        this.identityQueryService=identityQueryService;
        this.feeService=feeService;
    }

    /* bank officer or institution officer search for a guardian using national id */
    @PostMapping
    public List<CategorizedFeesResponse> searchFessByNationalId(
        @Valid @RequestBody FeeSearchRequest request
    ){
        identityAccessService.requireFeeSearchAccess();
        NationalId nationalId=new NationalId(request.nationalId());
        try{
        GuardianSearchScope guardianSearchScope=identityQueryService.resolveGuardianSearchScope(nationalId);
        AuthenticatedActor actor=identityAccessService.currentActor();
        if (actor.role() == RoleCode.INSTITUTION_OFFICER) {
            return buildInstitutionCategorizedFees(
                    guardianSearchScope.studentIds(),
                    actor.institutionId()
            );
        }
        return buildCategorizedFees(guardianSearchScope.studentIds());
        }catch(GuardianNotFoundException ex){
            return List.of();
        }
    }

    /*logged in gurdian gets fees for their students */
    @GetMapping("/me")
    public List<StudentFeeRecordResponse> getMyFees(){
        Set<UUID> studentIds=identityQueryService.getCurrentPayerStudentIds();
        Map<UUID, String> studentNames = identityQueryService.findStudentNames(studentIds);
        List<StudentFeeRecordResponse> response=new ArrayList<>();
        for(UUID studentId:studentIds){
            List<Fee> fees=feeService.getFeesForStudent(studentId);
            response.add(StudentFeeRecordResponse.from(
                    studentId,
                    studentNames.get(studentId),
                    institutionNameFor(fees, feeService.findInstitutionNames(institutionIdsFor(fees))),
                    fees));
        }
        return response;
    }

    /*build categorized response for each student */
    private List<CategorizedFeesResponse> buildCategorizedFees(
        Set<UUID> studentIds
    ){
        Map<UUID, String> studentNames = identityQueryService.findStudentNames(studentIds);
        List<CategorizedFeesResponse> response=new ArrayList<>();
        for(UUID studentId:studentIds){
            List<Fee> fees=feeService.getPayableFeesForStudent(studentId);
            if (fees.isEmpty()) {
                continue;
            }
            response.add(CategorizedFeesResponse.from(
                    studentId,
                    studentNames.get(studentId),
                    institutionNameFor(fees, feeService.findInstitutionNames(institutionIdsFor(fees))),
                    fees));
        }

        return response;
    }

    private List<CategorizedFeesResponse> buildInstitutionCategorizedFees(
        Set<UUID> studentIds,
        UUID institutionId
    ){
        Map<UUID, String> studentNames = identityQueryService.findStudentNames(studentIds);
        Map<UUID, String> institutionNames = feeService.findInstitutionNames(
                institutionId == null ? Set.of() : Set.of(institutionId));
        String institutionName = institutionNames.get(institutionId);
        List<CategorizedFeesResponse> response=new ArrayList<>();
        for(UUID studentId:studentIds){
            List<Fee> fees=feeService.findFeesForStudentAndInstitution(studentId, institutionId);
            if (fees.isEmpty()) {
                continue;
            }
            response.add(CategorizedFeesResponse.from(
                    studentId,
                    studentNames.get(studentId),
                    institutionName,
                    fees));
        }
        return response;
    }

    private Set<UUID> institutionIdsFor(List<Fee> fees) {
        return fees.stream().map(Fee::getInstitutionId).collect(Collectors.toSet());
    }

    private String institutionNameFor(List<Fee> fees, Map<UUID, String> institutionNames) {
        return fees.stream()
                .map(Fee::getInstitutionId)
                .map(institutionNames::get)
                .filter(name -> name != null && !name.isBlank())
                .distinct()
                .collect(Collectors.joining(", "));
    }
}
