package com.tuitionnetwork.tuitionnetwork.reporting.api;

import com.tuitionnetwork.tuitionnetwork.identity.domain.AuthenticatedActor;
import com.tuitionnetwork.tuitionnetwork.identity.domain.RoleCode;
import com.tuitionnetwork.tuitionnetwork.identity.service.IdentityAccessService;
import com.tuitionnetwork.tuitionnetwork.identity.service.IdentityQueryService;
import com.tuitionnetwork.tuitionnetwork.reporting.api.dto.DailyCollectionReportResponse;
import com.tuitionnetwork.tuitionnetwork.reporting.domain.DailyCollectionReport;
import com.tuitionnetwork.tuitionnetwork.reporting.service.DailyCollectionReportService;
import com.tuitionnetwork.tuitionnetwork.reporting.service.DailyCollectionCsvService;
import com.tuitionnetwork.tuitionnetwork.reporting.service.DailyCollectionPdfService;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class ReportingControllerTest {

    private DailyCollectionReportService reportService;
    private IdentityAccessService identityAccessService;
    private IdentityQueryService identityQueryService;
    private ReportingController controller;
    private DailyCollectionCsvService csvService;
    private DailyCollectionPdfService pdfService;

    @BeforeEach
    void setUp() {
        reportService = mock(DailyCollectionReportService.class);
        identityAccessService = mock(IdentityAccessService.class);
        identityQueryService = mock(IdentityQueryService.class);
        csvService=mock(DailyCollectionCsvService.class);
        pdfService=mock(DailyCollectionPdfService.class);

        controller = new ReportingController(
                reportService,
                identityAccessService,
                identityQueryService,
                csvService,
                pdfService
        );
        when(identityQueryService.findStudentNames(any())).thenReturn(Map.of());
    }

    @Test
    void getDailyCollectionReport_UsesAuthenticatedInstitution() {

        UUID institutionId = UUID.randomUUID();
        LocalDate date = LocalDate.of(2026, 9, 16);

        AuthenticatedActor actor = mock(AuthenticatedActor.class);

        when(actor.institutionId())
                .thenReturn(institutionId);
        when(actor.role())
                .thenReturn(RoleCode.INSTITUTION_OFFICER);

        when(identityAccessService.currentActor())
                .thenReturn(actor);

        DailyCollectionReport report =
                new DailyCollectionReport(
                        date,
                        BigDecimal.ZERO,
                        0,
                        Map.of(),
                        Map.of(),
                        List.of()
                );

        when(reportService.getDailyReport(
                institutionId,
                date
        )).thenReturn(report);

        DailyCollectionReportResponse response =
                controller.getDailyCollectionReport(date, null);

        assertEquals(date, response.date());
        assertEquals(institutionId, response.institutionId());
        assertEquals(0, response.paymentCount());
        assertEquals(0, BigDecimal.ZERO.compareTo(response.totalCollected()));

        verify(identityAccessService)
                .currentActor();

        verify(identityAccessService)
                .requireInstitutionAccess(institutionId);

        verify(reportService)
                .getDailyReport(institutionId, date);
    }

    @Test
    void bankOfficerCanReadNetworkDailyReport() {
        LocalDate date = LocalDate.of(2026, 9, 16);
        AuthenticatedActor actor = mock(AuthenticatedActor.class);
        when(actor.role()).thenReturn(RoleCode.BANK_OFFICER);
        when(identityAccessService.currentActor()).thenReturn(actor);
        DailyCollectionReport report = new DailyCollectionReport(
                date, BigDecimal.ZERO, 0, Map.of(), Map.of(), List.of());
        when(reportService.getDailyReport(null, date)).thenReturn(report);

        DailyCollectionReportResponse response = controller.getDailyCollectionReport(date, null);

        assertNull(response.institutionId());
        verify(identityAccessService).requireBackofficeStaff();
        verify(reportService).getDailyReport(null, date);
    }
}