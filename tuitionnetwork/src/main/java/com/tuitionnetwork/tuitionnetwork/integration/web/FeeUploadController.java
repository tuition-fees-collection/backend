package com.tuitionnetwork.tuitionnetwork.integration.web;

import com.tuitionnetwork.tuitionnetwork.identity.domain.AuthenticatedActor;
import com.tuitionnetwork.tuitionnetwork.identity.service.IdentityAccessService;
import com.tuitionnetwork.tuitionnetwork.integration.domain.UploadBatch;
import com.tuitionnetwork.tuitionnetwork.integration.service.FeeUploadResult;
import com.tuitionnetwork.tuitionnetwork.integration.service.FeeUploadService;
import com.tuitionnetwork.tuitionnetwork.integration.service.DuplicateUploadException;
import com.tuitionnetwork.tuitionnetwork.integration.service.UploadBatchNotFoundException;
import com.tuitionnetwork.tuitionnetwork.integration.service.UploadValidationException;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.List;
import java.util.UUID;

@RestController
@SecurityRequirement(name = "bearerAuth")
public class FeeUploadController {

    private final FeeUploadService feeUploadService;
    private final IdentityAccessService identity;

    public FeeUploadController(FeeUploadService feeUploadService, IdentityAccessService identity) {
        this.feeUploadService = feeUploadService;
        this.identity = identity;
    }

    @PostMapping("/api/v1/integration/uploads")
    public ResponseEntity<FeeUploadResult> upload(
            @RequestParam(required = false) String externalBatchReference,
            @RequestPart MultipartFile file) {
        AuthenticatedActor actor = identity.currentActor();
        identity.requireInstitutionAccess(actor.institutionId());
        try {
            FeeUploadResult result = feeUploadService.submit(
                    actor.institutionId(),
                    actor.actorId(),
                    file.getOriginalFilename(),
                    externalBatchReference,
                    file.getInputStream());
            return ResponseEntity.ok(result);
        } catch (IOException e) {
            throw new UncheckedIOException("Could not read uploaded file", e);
        }
    }

    @GetMapping("/api/v1/integration/uploads/{batchId}")
    public UploadBatchView getBatch(@PathVariable UUID batchId) {
        UploadBatch batch = feeUploadService.getBatch(batchId);
        identity.requireInstitutionAccess(batch.getInstitutionId());
        return UploadBatchView.from(batch);
    }

    @GetMapping("/api/v1/integration/uploads")
    public List<UploadBatchView> listUploads() {
        AuthenticatedActor actor = identity.currentActor();
        identity.requireInstitutionAccess(actor.institutionId());
        return feeUploadService.findByInstitution(actor.institutionId()).stream()
                .map(UploadBatchView::from)
                .toList();
    }

    @ExceptionHandler(UploadBatchNotFoundException.class)
    public ResponseEntity<UploadValidationErrorResponse> batchNotFound(UploadBatchNotFoundException exception) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(
                new UploadValidationErrorResponse("NOT_FOUND", exception.getMessage()));
    }

    @ExceptionHandler(DuplicateUploadException.class)
    public ResponseEntity<DuplicateUploadResponse> duplicateUpload(DuplicateUploadException exception) {
        return ResponseEntity.status(HttpStatus.CONFLICT).body(new DuplicateUploadResponse(
                "DUPLICATE_UPLOAD",
                exception.getMessage(),
                exception.existingBatchId(),
                exception.existingStatus(),
                exception.matchedBy()));
    }

    @ExceptionHandler(UploadValidationException.class)
    public ResponseEntity<UploadValidationErrorResponse> invalidUpload(UploadValidationException exception) {
        return ResponseEntity.badRequest().body(
                new UploadValidationErrorResponse(exception.code(), exception.getMessage()));
    }
}
