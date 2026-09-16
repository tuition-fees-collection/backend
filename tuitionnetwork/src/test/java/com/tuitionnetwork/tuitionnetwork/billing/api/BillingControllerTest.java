package com.tuitionnetwork.tuitionnetwork.billing.api;

import com.tuitionnetwork.tuitionnetwork.billing.api.dto.InstitutionCollectionResponse;
import com.tuitionnetwork.tuitionnetwork.billing.api.dto.StudentFeeRecordResponse;
import com.tuitionnetwork.tuitionnetwork.billing.domain.Fee;
import com.tuitionnetwork.tuitionnetwork.billing.domain.FeeStatus;
import com.tuitionnetwork.tuitionnetwork.billing.domain.FeeType;
import com.tuitionnetwork.tuitionnetwork.billing.service.FeeNotFoundException;
import com.tuitionnetwork.tuitionnetwork.billing.service.FeeService;
import com.tuitionnetwork.tuitionnetwork.identity.domain.ActorType;
import com.tuitionnetwork.tuitionnetwork.identity.domain.AuthenticatedActor;
import com.tuitionnetwork.tuitionnetwork.identity.domain.RoleCode;
import com.tuitionnetwork.tuitionnetwork.identity.service.IdentityAccessService;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

public class BillingControllerTest {
    private FeeService feeService;
    private IdentityAccessService identityAccessService;
    private BillingController billingController;

    @BeforeEach 
    void setUp(){
        feeService=mock(FeeService.class);
        identityAccessService=mock(IdentityAccessService.class);
        billingController=new BillingController(
                feeService,
                identityAccessService
        );
    }

    @Test 
    void testGetStudentFeeRecord(){
        UUID accountId=UUID.randomUUID();
        UUID officerId=UUID.randomUUID();
        UUID institutionId =UUID.randomUUID();
        UUID studentId=UUID.randomUUID();

        AuthenticatedActor actor = new AuthenticatedActor(
                accountId,
                officerId,
                ActorType.INSTITUTION_USER,
                RoleCode.INSTITUTION_OFFICER,
                institutionId,
                null
        );

        Fee tuitionFee = new Fee(
                studentId,
                institutionId,
                FeeType.TUITION,
                "2026-2027",
                "EGP",
                new BigDecimal("10000.00"),
                null
        );

        Fee busFee = new Fee(
                studentId,
                institutionId,
                FeeType.TRANSPORTATION,
                "2026-2027",
                "EGP",
                new BigDecimal("3000.00"),
                null
        );
        tuitionFee.applyPayment(
                new BigDecimal("4000.00")
        );

        when(identityAccessService.currentActor()).thenReturn(actor);
        when(feeService.getFeesForStudentAndInstitution(studentId, institutionId)).
        thenReturn(List.of(tuitionFee,busFee));

        StudentFeeRecordResponse result=billingController.getStudentFeeRecord(studentId);
        assertEquals(studentId, result.studentId());
        assertEquals(new BigDecimal("13000.00"),result.yearTotal());
        assertEquals(new BigDecimal("4000.00"),result.amountPaid());
        assertEquals(new BigDecimal("9000.00"),result.amountOutstanding());
        assertEquals(2, result.fees().size());

        verify(identityAccessService).currentActor();
        verify(identityAccessService).requireInstitutionAccess(institutionId);
        verify(feeService).getFeesForStudentAndInstitution(studentId, institutionId);


    }

    @Test 
    void testGetStudentFeeRecord_NotAccessibleToInstitution() {
        UUID accountId=UUID.randomUUID();
        UUID officerId=UUID.randomUUID();
        UUID institutionId =UUID.randomUUID();
        UUID studentId=UUID.randomUUID();

        AuthenticatedActor actor=new AuthenticatedActor(
                accountId,
                officerId,
                ActorType.INSTITUTION_USER,
                RoleCode.INSTITUTION_OFFICER,
                institutionId,
                null

        );
        when(identityAccessService.currentActor()).thenReturn(actor);
        when(feeService.getFeesForStudentAndInstitution(studentId, institutionId)).
        thenThrow(
        new FeeNotFoundException(
                "Student fee record not found"
        )
        );

        FeeNotFoundException exception=assertThrows(
            FeeNotFoundException.class,()->
        billingController.getStudentFeeRecord(studentId));

        assertEquals("Student fee record not found",exception.getMessage());

        verify(identityAccessService).requireInstitutionAccess(institutionId);
        verify(feeService).getFeesForStudentAndInstitution(studentId, institutionId);

    }

    @Test
void testGetInstitutionCollections() {

    UUID accountId = UUID.randomUUID();
    UUID officerId = UUID.randomUUID();
    UUID institutionId = UUID.randomUUID();
    UUID studentId = UUID.randomUUID();

    AuthenticatedActor actor = new AuthenticatedActor(
            accountId,
            officerId,
            ActorType.INSTITUTION_USER,
            RoleCode.INSTITUTION_OFFICER,
            institutionId,
            null
    );

    Fee fee = new Fee(
            studentId,
            institutionId,
            FeeType.TUITION,
            "2026-2027",
            "EGP",
            new BigDecimal("10000.00"),
            null
    );

    when(identityAccessService.currentActor())
            .thenReturn(actor);

    when(feeService.getInstitutionCollections(
            institutionId,
            FeeStatus.OUTSTANDING,
            null,
            null
    )).thenReturn(List.of(fee));

    List<InstitutionCollectionResponse> result =
            billingController.getInstitutionCollections(
                    FeeStatus.OUTSTANDING,
                    null,
                    null
            );

    assertEquals(1, result.size());

    InstitutionCollectionResponse response = result.get(0);

    assertEquals(studentId, response.studentId());
    assertEquals(FeeType.TUITION, response.feeType());
    assertEquals("2026-2027", response.collectionPeriod());
    assertEquals(new BigDecimal("10000.00"), response.amount());
    assertEquals(BigDecimal.ZERO, response.amountPaid());
    assertEquals(new BigDecimal("10000.00"), response.outstandingAmount());
    assertEquals(FeeStatus.OUTSTANDING, response.feeStatus());

    verify(identityAccessService).currentActor();

    verify(identityAccessService)
            .requireInstitutionAccess(institutionId);

    verify(feeService).getInstitutionCollections(
            institutionId,
            FeeStatus.OUTSTANDING,
            null,
            null
    );
}

@Test
void testGetInstitutionCollections_WithFilters() {

    UUID accountId = UUID.randomUUID();
    UUID officerId = UUID.randomUUID();
    UUID institutionId = UUID.randomUUID();
    UUID studentId = UUID.randomUUID();

    AuthenticatedActor actor = new AuthenticatedActor(
            accountId,
            officerId,
            ActorType.INSTITUTION_USER,
            RoleCode.INSTITUTION_OFFICER,
            institutionId,
            null
    );

    Fee fee = new Fee(
            studentId,
            institutionId,
            FeeType.TUITION,
            "2026-2027",
            "EGP",
            new BigDecimal("10000.00"),
            null
    );

    fee.applyPayment(new BigDecimal("3000.00"));

    when(identityAccessService.currentActor())
            .thenReturn(actor);

    when(feeService.getInstitutionCollections(
            institutionId,
            FeeStatus.PARTIALLY_PAID,
            FeeType.TUITION,
            "2026-2027"
    )).thenReturn(List.of(fee));

    List<InstitutionCollectionResponse> result =
            billingController.getInstitutionCollections(
                    FeeStatus.PARTIALLY_PAID,
                    FeeType.TUITION,
                    "2026-2027"
            );

    assertEquals(1, result.size());

    InstitutionCollectionResponse response = result.get(0);

    assertEquals(FeeStatus.PARTIALLY_PAID, response.feeStatus());
    assertEquals(FeeType.TUITION, response.feeType());
    assertEquals("2026-2027", response.collectionPeriod());
    assertEquals(new BigDecimal("10000.00"), response.amount());
    assertEquals(new BigDecimal("3000.00"), response.amountPaid());
    assertEquals(new BigDecimal("7000.00"), response.outstandingAmount());

    verify(identityAccessService)
            .requireInstitutionAccess(institutionId);

    verify(feeService).getInstitutionCollections(
            institutionId,
            FeeStatus.PARTIALLY_PAID,
            FeeType.TUITION,
            "2026-2027"
    );
}
    
}
