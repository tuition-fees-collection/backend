package com.tuitionnetwork.tuitionnetwork.billing.api;

import com.tuitionnetwork.tuitionnetwork.billing.api.dto.StudentReceiptResponse;
import com.tuitionnetwork.tuitionnetwork.billing.domain.Fee;
import com.tuitionnetwork.tuitionnetwork.billing.domain.FeeStatus;
import com.tuitionnetwork.tuitionnetwork.billing.domain.FeeType;
import com.tuitionnetwork.tuitionnetwork.billing.service.FeeService;
import com.tuitionnetwork.tuitionnetwork.billing.service.StudentReceiptService;
import com.tuitionnetwork.tuitionnetwork.payment.domain.PaymentMethod;
import com.tuitionnetwork.tuitionnetwork.identity.domain.ActorType;
import com.tuitionnetwork.tuitionnetwork.identity.domain.AuthenticatedActor;
import com.tuitionnetwork.tuitionnetwork.identity.domain.RoleCode;
import com.tuitionnetwork.tuitionnetwork.identity.service.IdentityAccessService;
import com.tuitionnetwork.tuitionnetwork.identity.service.IdentityQueryService;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(BillingController.class)
class BillingControllerWebTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private FeeService feeService;

    @MockitoBean
    private StudentReceiptService studentReceiptService;

    @MockitoBean
    private IdentityAccessService identityAccessService;

    @MockitoBean
    private IdentityQueryService identityQueryService;

    @BeforeEach
    void stubNames() {
        when(identityQueryService.findStudentNames(any())).thenReturn(java.util.Map.of());
        when(feeService.findInstitutionNames(any())).thenReturn(java.util.Map.of());
    }

    @Test
    void getInstitutionCollections_ReturnsOutstandingFees() throws Exception {

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

        mockMvc.perform(
        get("/api/v1/billing/collections")
                .with(jwt())
                .param("status", "OUTSTANDING")
)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].studentId")
                        .value(studentId.toString()))
                .andExpect(jsonPath("$[0].feeType")
                        .value("TUITION"))
                .andExpect(jsonPath("$[0].collectionPeriod")
                        .value("2026-2027"))
                .andExpect(jsonPath("$[0].amount")
                        .value(10000))
                .andExpect(jsonPath("$[0].amountPaid")
                        .value(0))
                .andExpect(jsonPath("$[0].outstandingAmount")
                        .value(10000))
                .andExpect(jsonPath("$[0].feeStatus")
                        .value("OUTSTANDING"));

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
void getInstitutionCollections_WithFilters_ReturnsMatchingFees() throws Exception {

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

    mockMvc.perform(
            get("/api/v1/billing/collections")
                    .with(jwt())
                    .param("status", "PARTIALLY_PAID")
                    .param("feeType", "TUITION")
                    .param("collectionPeriod", "2026-2027")
    )
            .andExpect(status().isOk())
            .andExpect(jsonPath("$[0].studentId")
                    .value(studentId.toString()))
            .andExpect(jsonPath("$[0].feeType")
                    .value("TUITION"))
            .andExpect(jsonPath("$[0].collectionPeriod")
                    .value("2026-2027"))
            .andExpect(jsonPath("$[0].amount")
                    .value(10000))
            .andExpect(jsonPath("$[0].amountPaid")
                    .value(3000))
            .andExpect(jsonPath("$[0].outstandingAmount")
                    .value(7000))
            .andExpect(jsonPath("$[0].feeStatus")
                    .value("PARTIALLY_PAID"));

    verify(identityAccessService)
            .requireInstitutionAccess(institutionId);

    verify(feeService).getInstitutionCollections(
            institutionId,
            FeeStatus.PARTIALLY_PAID,
            FeeType.TUITION,
            "2026-2027"
    );
}

@Test
void getInstitutionCollections_WithoutStatus_ReturnsBadRequest() throws Exception {

    mockMvc.perform(
            get("/api/v1/billing/collections")
                    .with(jwt())
    )
            .andExpect(status().isBadRequest());

    verifyNoInteractions(feeService);
}

@Test
void getStudentReceipts_ReturnsIssuedReceipts() throws Exception {
    UUID accountId = UUID.randomUUID();
    UUID officerId = UUID.randomUUID();
    UUID institutionId = UUID.randomUUID();
    UUID studentId = UUID.randomUUID();
    UUID paymentId = UUID.randomUUID();
    UUID feeId = UUID.randomUUID();

    AuthenticatedActor actor = new AuthenticatedActor(
            accountId,
            officerId,
            ActorType.INSTITUTION_USER,
            RoleCode.INSTITUTION_OFFICER,
            institutionId,
            null
    );

    when(identityAccessService.currentActor()).thenReturn(actor);
    when(studentReceiptService.getStudentReceipts(studentId, institutionId)).thenReturn(List.of(
            new StudentReceiptResponse(
                    paymentId,
                    "RCP-2026-WALK0001",
                    Instant.parse("2026-09-18T10:00:00Z"),
                    new BigDecimal("8000.00"),
                    "EGP",
                    PaymentMethod.CASH,
                    null,
                    null,
                    List.of(new StudentReceiptResponse.Line(feeId, "TUITION", "2026-09", new BigDecimal("8000.00")))
            )
    ));
    when(identityQueryService.findStudentNames(any())).thenReturn(Map.of(studentId, "Nour Khaled Fahmy"));
    when(feeService.findInstitutionNames(any())).thenReturn(Map.of(institutionId, "Nile Valley School"));

    mockMvc.perform(
            get("/api/v1/billing/students/{studentId}/receipts", studentId)
                    .with(jwt())
    )
            .andExpect(status().isOk())
            .andExpect(jsonPath("$[0].referenceNumber").value("RCP-2026-WALK0001"))
            .andExpect(jsonPath("$[0].studentName").value("Nour Khaled Fahmy"))
            .andExpect(jsonPath("$[0].institutionName").value("Nile Valley School"))
            .andExpect(jsonPath("$[0].lines[0].feeType").value("TUITION"));

    verify(identityAccessService).requireInstitutionAccess(institutionId);
    verify(studentReceiptService).getStudentReceipts(studentId, institutionId);
}
}