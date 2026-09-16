package com.tuitionnetwork.tuitionnetwork.reporting.api;

import com.tuitionnetwork.tuitionnetwork.identity.domain.AuthenticatedActor;
import com.tuitionnetwork.tuitionnetwork.identity.service.IdentityAccessService;
import com.tuitionnetwork.tuitionnetwork.reporting.api.dto.DailyCollectionReportResponse;
import com.tuitionnetwork.tuitionnetwork.reporting.domain.DailyCollectionReport;
import com.tuitionnetwork.tuitionnetwork.reporting.service.DailyCollectionCsvService;
import com.tuitionnetwork.tuitionnetwork.reporting.service.DailyCollectionReportService;
import com.tuitionnetwork.tuitionnetwork.reporting.service.DailyCollectionPdfService;

import io.swagger.v3.oas.annotations.security.SecurityRequirement;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.UUID;

import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;

@RestController 
@RequestMapping("/api/v1/reporting")
@SecurityRequirement(name="bearerAuth")
public class ReportingController{

    private final DailyCollectionReportService reportService;
    private final IdentityAccessService identityAccessService;
    private final DailyCollectionCsvService csvService;
    private final DailyCollectionPdfService pdfService;

    public ReportingController(
            DailyCollectionReportService reportService,
            IdentityAccessService identityAccessService,
            DailyCollectionCsvService csvService,
            DailyCollectionPdfService pdfService
    ) {
        this.reportService = reportService;
        this.identityAccessService = identityAccessService;
        this.csvService = csvService;
        this.pdfService=pdfService;
    }
     @GetMapping("/daily-collections")
    public DailyCollectionReportResponse getDailyCollectionReport(
            @RequestParam
            @DateTimeFormat(iso=DateTimeFormat.ISO.DATE)
            LocalDate date
    ) {

        AuthenticatedActor actor=identityAccessService.currentActor();

        UUID institutionId=actor.institutionId();

        identityAccessService.requireInstitutionAccess(
                institutionId
        );

         DailyCollectionReport report=reportService.getDailyReport(
                        institutionId,
                        date
                    );

        return DailyCollectionReportResponse.from(
                institutionId,
                report
        );
    
    }
    @GetMapping("/daily-collections/csv")
    public ResponseEntity<byte[]> downloadDailyCollectionCsv(
        @RequestParam
        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
        LocalDate date
) {

    AuthenticatedActor actor=identityAccessService.currentActor();
    UUID institutionId=actor.institutionId();
    identityAccessService.requireInstitutionAccess(institutionId);
    DailyCollectionReport report=reportService.getDailyReport(institutionId, date);
    byte[] csv=csvService.generateCsv(report);
    String fileName= "daily-collections-"+ date+".csv";
    return  ResponseEntity.ok()
    .header(HttpHeaders.CONTENT_DISPOSITION,"attachment; filename=\""+fileName+ "\"")
    .contentType(MediaType.parseMediaType("text/csv")).body(csv);
}

 @GetMapping("/daily-collections/pdf")
    public ResponseEntity<byte[]> downloadDailyCollectionPdf(
        @RequestParam
        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
        LocalDate date
) {

    AuthenticatedActor actor=identityAccessService.currentActor();
    UUID institutionId=actor.institutionId();
    identityAccessService.requireInstitutionAccess(institutionId);
    DailyCollectionReport report=reportService.getDailyReport(institutionId, date);
    byte[] pdf=pdfService.generatePdf(report);
    String fileName= "daily-collections-"+ date+".pdf";
    return  ResponseEntity.ok()
    .header(HttpHeaders.CONTENT_DISPOSITION,"attachment; filename=\""+fileName+ "\"")
    .contentType(MediaType.APPLICATION_PDF).body(pdf);
}

}