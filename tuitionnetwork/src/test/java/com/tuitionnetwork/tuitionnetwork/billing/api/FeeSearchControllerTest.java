package com.tuitionnetwork.tuitionnetwork.billing.api;

import com.tuitionnetwork.tuitionnetwork.billing.api.dto.CategorizedFeesResponse;
import com.tuitionnetwork.tuitionnetwork.billing.api.dto.FeeSearchRequest;
import com.tuitionnetwork.tuitionnetwork.billing.domain.Fee;
import com.tuitionnetwork.tuitionnetwork.billing.domain.FeeType;
import com.tuitionnetwork.tuitionnetwork.billing.service.FeeService;
import com.tuitionnetwork.tuitionnetwork.billing.api.dto.StudentFeeRecordResponse;

import com.tuitionnetwork.tuitionnetwork.identity.domain.NationalId;
import com.tuitionnetwork.tuitionnetwork.identity.service.GuardianSearchScope;
import com.tuitionnetwork.tuitionnetwork.identity.service.IdentityAccessService;
import com.tuitionnetwork.tuitionnetwork.identity.service.IdentityQueryService;
import com.tuitionnetwork.tuitionnetwork.identity.service.GuardianNotFoundException;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

 class FeeSearchControllerTest {
    private IdentityAccessService identityAccessService;
    private IdentityQueryService identityQueryService;
    private FeeService feeService;
    private FeeSearchController feeSearchController;

    @BeforeEach
    void setUp(){
        identityAccessService = mock(IdentityAccessService.class);
        identityQueryService=mock(IdentityQueryService.class);
        feeService=mock(FeeService.class);
        feeSearchController=new FeeSearchController(
            identityAccessService,
            identityQueryService,
             feeService);
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
        verify(identityAccessService).requireBankOfficer();
        verify(identityQueryService).resolveGuardianSearchScope(
            new NationalId(feeSearchRequest.nationalId())
        );

        verify(feeService).getPayableFeesForStudent(studentId);
    }

    @Test
    void testGetMyFees(){
        UUID studentId=UUID.randomUUID();
        UUID institutionId=UUID.randomUUID();
        Fee fee=new Fee(
            studentId,
            institutionId,
            FeeType.TUITION,
            "2026-2027",
            "EGP",
            new BigDecimal("8000.00"),
            null

        );
        Fee settledFee=new Fee(
            studentId,
            institutionId,
            FeeType.ACTIVITIES,
            "2026-2027",
            "EGP",
            new BigDecimal("2000.00"),
            null

        );
        settledFee.applyPayment(new BigDecimal("2000.00"));
        when(identityQueryService.getCurrentPayerStudentIds()).thenReturn(Set.of(studentId));

        when(feeService.getFeesForStudent(studentId)).thenReturn(List.of(fee,settledFee));

    List<StudentFeeRecordResponse> result=feeSearchController.getMyFees();

    assertEquals(1, result.size());

    assertEquals(
            studentId,
            result.get(0).studentId()
    );
    assertEquals(
            new BigDecimal("10000.00"),
            result.get(0).yearTotal()
    );
    assertEquals(
            new BigDecimal("2000.00"),
            result.get(0).amountPaid()
    );
    assertEquals(
            new BigDecimal("8000.00"),
            result.get(0).amountOutstanding()
    );
    assertEquals(
            2,
            result.get(0).fees().size()
    );

    verify(identityQueryService)
            .getCurrentPayerStudentIds();

    verify(feeService)
            .getFeesForStudent(studentId);

    verify(feeService,never())
            .getPayableFeesForStudent(studentId);


    verify(identityAccessService, never())
            .requireBankOfficer();

    }

    @Test
void testSearchFeesByNationalIdReturnsEmptyListWhenGuardianNotFound() {
    FeeSearchRequest request =
            new FeeSearchRequest("29805150101023");

    when(identityQueryService.resolveGuardianSearchScope(
            new NationalId(request.nationalId())
    )).thenThrow(new GuardianNotFoundException());

    List<CategorizedFeesResponse> result =
            feeSearchController.searchFessByNationalId(request);

    assertTrue(result.isEmpty());

    verify(identityAccessService).requireBankOfficer();

    verify(identityQueryService).resolveGuardianSearchScope(
            new NationalId(request.nationalId())
    );

    verifyNoInteractions(feeService);
}

    
}