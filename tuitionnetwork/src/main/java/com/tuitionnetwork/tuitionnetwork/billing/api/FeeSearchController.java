package com.tuitionnetwork.tuitionnetwork.billing.api;
import com.tuitionnetwork.tuitionnetwork.billing.domain.Fee;
import com.tuitionnetwork.tuitionnetwork.billing.api.dto.FeeSearchRequest;
import com.tuitionnetwork.tuitionnetwork.billing.service.FeeService;
import com.tuitionnetwork.tuitionnetwork.billing.api.dto.CategorizedFeesResponse;


import java.util.List;
import java.util.UUID;
import java.util.Set;
import java.util.ArrayList;

import jakarta.validation.Valid;

import com.tuitionnetwork.tuitionnetwork.identity.domain.NationalId;
import com.tuitionnetwork.tuitionnetwork.identity.service.GuardianSearchScope;
import com.tuitionnetwork.tuitionnetwork.identity.service.IdentityAccessService;
import com.tuitionnetwork.tuitionnetwork.identity.service.IdentityQueryService;

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

    /*bank officer serach for a gurdian using national id */
    @PostMapping
    public List<CategorizedFeesResponse> searchFessByNationalId(
        @Valid @RequestBody FeeSearchRequest request
    ){
        identityAccessService.requireBankOfficer();
        NationalId nationalId=new NationalId(request.nationalId());
        GuardianSearchScope guardianSearchScope=identityQueryService.resolveGuardianSearchScope(nationalId);
        return buildCategorizedFees(guardianSearchScope.studentIds());
    }

    /*logged in gurdian gets fees for their students */
    @GetMapping("/me")
    public List<CategorizedFeesResponse> getMyFees(){
        Set<UUID> studentIds=identityQueryService.getCurrentPayerStudentIds();
        return buildCategorizedFees(studentIds);
    }

    /*build categorized response for each student */
    private List<CategorizedFeesResponse> buildCategorizedFees(
        Set<UUID> studentIds
    ){
        List<CategorizedFeesResponse> response=new ArrayList<>();
        for(UUID studentId:studentIds){
            List<Fee> fees=feeService.getPayableFeesForStudent(studentId);
            response.add( CategorizedFeesResponse.from(studentId,fees));
        }

        return response;
    }


    
    
}