package com.tuitionnetwork.tuitionnetwork.reporting.api;

import com.tuitionnetwork.tuitionnetwork.billing.domain.FeeType;
import com.tuitionnetwork.tuitionnetwork.identity.domain.ActorType;
import com.tuitionnetwork.tuitionnetwork.identity.domain.AuthenticatedActor;
import com.tuitionnetwork.tuitionnetwork.identity.domain.RoleCode;
import com.tuitionnetwork.tuitionnetwork.identity.service.IdentityAccessService;
import com.tuitionnetwork.tuitionnetwork.payment.domain.PaymentMethod;
import com.tuitionnetwork.tuitionnetwork.reporting.domain.DailyCollectionRecord;
import com.tuitionnetwork.tuitionnetwork.reporting.domain.DailyCollectionReport;
import com.tuitionnetwork.tuitionnetwork.reporting.service.DailyCollectionCsvService;
import com.tuitionnetwork.tuitionnetwork.reporting.service.DailyCollectionReportService;
import com.tuitionnetwork.tuitionnetwork.reporting.service.DailyCollectionPdfService;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import org.springframework.http.HttpHeaders;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(ReportingController.class)
class ReportingControllerWebTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private DailyCollectionReportService reportService;

    @MockitoBean
    private IdentityAccessService identityAccessService;

    @MockitoBean
    private DailyCollectionCsvService csvService;

    @MockitoBean
    private DailyCollectionPdfService pdfService;

    @Test
    void getDailyCollectionReport_ReturnsInstitutionReport() throws Exception {

        UUID accountId = UUID.randomUUID();
        UUID officerId = UUID.randomUUID();
        UUID institutionId = UUID.randomUUID();
        UUID studentId = UUID.randomUUID();
        UUID paymentId = UUID.randomUUID();

        LocalDate date = LocalDate.of(2026, 9, 16);

        AuthenticatedActor actor = new AuthenticatedActor(
                accountId,
                officerId,
                ActorType.INSTITUTION_USER,
                RoleCode.INSTITUTION_OFFICER,
                institutionId,
                null
        );

        DailyCollectionRecord item =
                new DailyCollectionRecord(
                        paymentId,
                        studentId,
                        FeeType.TUITION,
                        new BigDecimal("4000.00"),
                        PaymentMethod.CREDIT_CARD,
                        Instant.parse("2026-09-16T10:00:00Z"),
                        "3355"
                );

        DailyCollectionReport report =
                new DailyCollectionReport(
                        date,
                        new BigDecimal("4000.00"),
                        1,
                        Map.of(
                                FeeType.TUITION,
                                new BigDecimal("4000.00")
                        ),
                        Map.of(
                                PaymentMethod.CREDIT_CARD,
                                new BigDecimal("4000.00")
                        ),
                        List.of(item)
                );

        when(identityAccessService.currentActor())
                .thenReturn(actor);

        when(reportService.getDailyReport(
                institutionId,
                date
        )).thenReturn(report);

        mockMvc.perform(
                get("/api/v1/reporting/daily-collections")
                        .with(jwt())
                        .param("date", "2026-09-16")
        )
                .andExpect(status().isOk())

                .andExpect(jsonPath("$.date")
                        .value("2026-09-16"))

                .andExpect(jsonPath("$.institutionId")
                        .value(institutionId.toString()))

                .andExpect(jsonPath("$.totalCollected")
                        .value(4000))

                .andExpect(jsonPath("$.paymentCount")
                        .value(1))

                .andExpect(jsonPath("$.breakdownByFeeType.TUITION")
                        .value(4000))

                .andExpect(jsonPath("$.breakdownByPaymentMethod.CREDIT_CARD")
                        .value(4000))

                .andExpect(jsonPath("$.items[0].studentId")
                        .value(studentId.toString()))

                .andExpect(jsonPath("$.items[0].feeType")
                        .value("TUITION"))

                .andExpect(jsonPath("$.items[0].amount")
                        .value(4000))

                .andExpect(jsonPath("$.items[0].paymentMethod")
                        .value("CREDIT_CARD"))

                .andExpect(jsonPath("$.items[0].settledAt")
                        .value("2026-09-16T10:00:00Z"))

                .andExpect(jsonPath("$.items[0].paymentReference")
                        .value("3355"));

        verify(identityAccessService)
                .requireInstitutionAccess(institutionId);

        verify(reportService)
                .getDailyReport(institutionId, date);
    }

    @Test
void downloadDailyCollectionCsv_ReturnsCsvAttachment() throws Exception {

    UUID accountId = UUID.randomUUID();
    UUID officerId = UUID.randomUUID();
    UUID institutionId = UUID.randomUUID();

    LocalDate date = LocalDate.of(2026, 9, 16);

    AuthenticatedActor actor = new AuthenticatedActor(
            accountId,
            officerId,
            ActorType.INSTITUTION_USER,
            RoleCode.INSTITUTION_OFFICER,
            institutionId,
            null
    );

    DailyCollectionReport report =
            new DailyCollectionReport(
                    date,
                    new BigDecimal("4000.00"),
                    1,
                    Map.of(
                            FeeType.TUITION,
                            new BigDecimal("4000.00")
                    ),
                    Map.of(
                            PaymentMethod.CREDIT_CARD,
                            new BigDecimal("4000.00")
                    ),
                    List.of()
            );

    String csv =
            "Student ID,Fee Type,Amount,Payment Method,Settled At,Payment Reference\n";

    when(identityAccessService.currentActor())
            .thenReturn(actor);

    when(reportService.getDailyReport(
            institutionId,
            date
    )).thenReturn(report);

    when(csvService.generateCsv(report))
            .thenReturn(csv.getBytes(StandardCharsets.UTF_8));

    mockMvc.perform(
            get("/api/v1/reporting/daily-collections/csv")
                    .with(jwt())
                    .param("date", "2026-09-16")
    )
            .andExpect(status().isOk())

            .andExpect(content().contentTypeCompatibleWith("text/csv"))

            .andExpect(header().string(
                    HttpHeaders.CONTENT_DISPOSITION,
                    "attachment; filename=\"daily-collections-2026-09-16.csv\""
            ))

            .andExpect(content().string(csv));

    verify(identityAccessService)
            .requireInstitutionAccess(institutionId);

    verify(reportService)
            .getDailyReport(institutionId, date);

    verify(csvService)
            .generateCsv(report);
}

@Test
void downloadDailyCollectionPdf_ReturnsPdfAttachment() throws Exception {

    UUID accountId = UUID.randomUUID();
    UUID officerId = UUID.randomUUID();
    UUID institutionId = UUID.randomUUID();

    LocalDate date = LocalDate.of(2026, 9, 16);

    AuthenticatedActor actor = new AuthenticatedActor(
            accountId,
            officerId,
            ActorType.INSTITUTION_USER,
            RoleCode.INSTITUTION_OFFICER,
            institutionId,
            null
    );

    DailyCollectionReport report =
            new DailyCollectionReport(
                    date,
                    new BigDecimal("4000.00"),
                    1,
                    Map.of(
                            FeeType.TUITION,
                            new BigDecimal("4000.00")
                    ),
                    Map.of(
                            PaymentMethod.CREDIT_CARD,
                            new BigDecimal("4000.00")
                    ),
                    List.of()
            );

    byte[] pdf = "%PDF-test".getBytes(StandardCharsets.UTF_8);

    when(identityAccessService.currentActor())
            .thenReturn(actor);

    when(reportService.getDailyReport(
            institutionId,
            date
    )).thenReturn(report);

    when(pdfService.generatePdf(report))
            .thenReturn(pdf);

    mockMvc.perform(
            get("/api/v1/reporting/daily-collections/pdf")
                    .with(jwt())
                    .param("date", "2026-09-16")
    )
            .andExpect(status().isOk())

            .andExpect(content()
                    .contentTypeCompatibleWith("application/pdf"))

            .andExpect(header().string(
                    HttpHeaders.CONTENT_DISPOSITION,
                    "attachment; filename=\"daily-collections-2026-09-16.pdf\""
            ))

            .andExpect(content().bytes(pdf));

    verify(identityAccessService)
            .requireInstitutionAccess(institutionId);

    verify(reportService)
            .getDailyReport(institutionId, date);

    verify(pdfService)
            .generatePdf(report);
}

    
}