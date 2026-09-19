package com.tuitionnetwork.tuitionnetwork.billing.api;

import com.tuitionnetwork.tuitionnetwork.billing.api.dto.CategorizedFeesResponse;
import com.tuitionnetwork.tuitionnetwork.billing.api.dto.FeeSearchRequest;
import com.tuitionnetwork.tuitionnetwork.billing.domain.Fee;
import com.tuitionnetwork.tuitionnetwork.billing.domain.FeeType;
import com.tuitionnetwork.tuitionnetwork.billing.service.FeeService;
import com.tuitionnetwork.tuitionnetwork.billing.api.dto.StudentFeeRecordResponse;

import com.tuitionnetwork.tuitionnetwork.identity.domain.ActorType;
import com.tuitionnetwork.tuitionnetwork.identity.domain.AuthenticatedActor;
import com.tuitionnetwork.tuitionnetwork.identity.domain.NationalId;
import com.tuitionnetwork.tuitionnetwork.identity.domain.RoleCode;
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
        when(identityQueryService.findStudentNames(any())).thenReturn(java.util.Map.of());
        when(feeService.findInstitutionNames(any())).thenReturn(java.util.Map.of());
    }

    @Test
    void testSearchFeesByNationalId(){
        UUID GuardianId=UUID.randomUUID();
        UUID studentId=UUID.randomUUID();
        UUID institutionId=UUID.randomUUID();
        when(identityAccessService.currentActor()).thenReturn(bankOfficer());
             
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
        verify(identityAccessService).requireFeeSearchAccess();
        verify(identityQueryService).resolveGuardianSearchScope(
            new NationalId(feeSearchRequest.nationalId())
        );

        verify(feeService).getPayableFeesForStudent(studentId);
        verify(feeService, never()).findFeesForStudentAndInstitution(any(), any());
    }

    @Test
    void searchIncludesStudentAndInstitutionNames() {
        UUID guardianId = UUID.randomUUID();
        UUID studentId = UUID.randomUUID();
        UUID institutionId = UUID.randomUUID();
        when(identityAccessService.currentActor()).thenReturn(bankOfficer());
        when(identityQueryService.findStudentNames(Set.of(studentId)))
                .thenReturn(java.util.Map.of(studentId, "Nour Khaled Fahmy"));
        when(feeService.findInstitutionNames(Set.of(institutionId)))
                .thenReturn(java.util.Map.of(institutionId, "Sunrise International School"));

        Fee fee = new Fee(
                studentId,
                institutionId,
                FeeType.TUITION,
                "2026-2027",
                "EGP",
                new BigDecimal("10000.00"),
                null
        );
        when(identityQueryService.resolveGuardianSearchScope(new NationalId("29805150101023")))
                .thenReturn(new GuardianSearchScope(guardianId, Set.of(studentId)));
        when(feeService.getPayableFeesForStudent(studentId)).thenReturn(List.of(fee));

        List<CategorizedFeesResponse> result = feeSearchController.searchFessByNationalId(
                new FeeSearchRequest("29805150101023"));

        assertEquals("Nour Khaled Fahmy", result.get(0).studentName());
        assertEquals("Sunrise International School", result.get(0).institutionName());
    }

    @Test
    void institutionOfficerSearchReturnsOnlyStudentsAtTheirInstitution() {
        UUID guardianId = UUID.randomUUID();
        UUID ownStudentId = UUID.randomUUID();
        UUID otherStudentId = UUID.randomUUID();
        UUID institutionId = UUID.randomUUID();
        when(identityAccessService.currentActor()).thenReturn(institutionOfficer(institutionId));

        FeeSearchRequest request = new FeeSearchRequest("29805150101023");
        when(identityQueryService.resolveGuardianSearchScope(new NationalId(request.nationalId())))
                .thenReturn(new GuardianSearchScope(guardianId, Set.of(ownStudentId, otherStudentId)));

        Fee ownFee = new Fee(
                ownStudentId,
                institutionId,
                FeeType.TUITION,
                "2026-2027",
                "EGP",
                new BigDecimal("5000.00"),
                null
        );
        when(feeService.findFeesForStudentAndInstitution(ownStudentId, institutionId))
                .thenReturn(List.of(ownFee));
        when(feeService.findFeesForStudentAndInstitution(otherStudentId, institutionId))
                .thenReturn(List.of());

        List<CategorizedFeesResponse> result = feeSearchController.searchFessByNationalId(request);

        assertEquals(1, result.size());
        assertEquals(ownStudentId, result.get(0).studentId());
        assertEquals(new BigDecimal("5000.00"), result.get(0).totalOutstandingAmount());
        verify(identityAccessService).requireFeeSearchAccess();
        verify(feeService, never()).getPayableFeesForStudent(any());
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
            .requireFeeSearchAccess();

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

    verify(identityAccessService).requireFeeSearchAccess();

    verify(identityQueryService).resolveGuardianSearchScope(
            new NationalId(request.nationalId())
    );

    verifyNoInteractions(feeService);
}

    private AuthenticatedActor bankOfficer() {
        return new AuthenticatedActor(
                UUID.randomUUID(),
                UUID.randomUUID(),
                ActorType.BACKOFFICE_EMPLOYEE,
                RoleCode.BANK_OFFICER,
                null,
                "Giza"
        );
    }

    private AuthenticatedActor institutionOfficer(UUID institutionId) {
        return new AuthenticatedActor(
                UUID.randomUUID(),
                UUID.randomUUID(),
                ActorType.INSTITUTION_USER,
                RoleCode.INSTITUTION_OFFICER,
                institutionId,
                null
        );
    }
}
