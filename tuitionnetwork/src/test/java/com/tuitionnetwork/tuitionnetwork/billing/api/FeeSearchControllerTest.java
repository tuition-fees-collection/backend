package com.tuitionnetwork.tuitionnetwork.billing.api;

import com.tuitionnetwork.tuitionnetwork.billing.api.dto.CategorizedFeesResponse;
import com.tuitionnetwork.tuitionnetwork.billing.api.dto.FeeSearchRequest;
import com.tuitionnetwork.tuitionnetwork.billing.domain.Fee;
import com.tuitionnetwork.tuitionnetwork.billing.domain.FeeType;
import com.tuitionnetwork.tuitionnetwork.billing.service.FeeService;

import com.tuitionnetwork.tuitionnetwork.identity.domain.NationalId;
import com.tuitionnetwork.tuitionnetwork.identity.service.GuardianSearchScope;
import com.tuitionnetwork.tuitionnetwork.identity.service.IdentityQueryService;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

 class FeeSearchControllerTest {

    private IdentityQueryService identityQueryService;
    private FeeService feeService;
    private FeeSearchController feeSearchController;

    @BeforeEach
    void setUp(){
        IdentityQueryService identityQueryService=mock(IdentityQueryService.class);
        feeService=mock(FeeService.class);
        feeSearchController=new FeeSearchController(identityQueryService, feeService);
    }

    @Test
    void testSearchFeesByNationalId(){
        UUID GuardianId=UUID.randomUUID();
        UUID studentId=UUID.randomUUID();
        UUID institutionId=UUID.randomUUID();
             
        FeeSearchRequest feeSearchRequest=new FeeSearchRequest("29805150101023");
        GuardianSearchScope guardianSearchScope=new GuardianSearchScope(GuardianId,Set.of(studentId));

        Fee fee=new Fee(
            studentId,
            institutionId,
            FeeType.TUITION,
            "2026-2027",
            "EGP",
            new BigDecimal("10000.00"),
            null

        );
        when(
            identityQueryService.resolveGuardianSearchScope(
                new NationalId(feeSearchRequest.nationalId())

        )).thenReturn(guardianSearchScope);

        when(
            feeService.getPayableFeesForStudent(studentId)
        ).thenReturn(List.of(fee));

        List<CategorizedFeesResponse> result=feeSearchController.searchFessByNationalId(feeSearchRequest);
        assertEquals(1, result.size());
        assertEquals(studentId,result.get(0).studentId());
        assertEquals(
            new BigDecimal("10000.00"),
            result.get(0).totalOutstandingAmount()
        );
        verify(identityQueryService).resolveGuardianSearchScope(
            new NationalId(feeSearchRequest.nationalId())
        );

        verify(feeService).getPayableFeesForStudent(studentId);
    }

    
}
