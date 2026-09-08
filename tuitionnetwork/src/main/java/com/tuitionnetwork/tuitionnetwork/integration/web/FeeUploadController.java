package com.tuitionnetwork.tuitionnetwork.integration.web;

import com.tuitionnetwork.tuitionnetwork.integration.service.FeeUploadResult;
import com.tuitionnetwork.tuitionnetwork.integration.service.FeeUploadService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.UUID;

@RestController
public class FeeUploadController {

    private final FeeUploadService feeUploadService;

    public FeeUploadController(FeeUploadService feeUploadService) {
        this.feeUploadService = feeUploadService;
    }

    // NOTE
    // institutionId/uploadedBy are request params for now and will be changed when institution auth exists
    // and come from the authenticated institution_user's session instead.
    @PostMapping("/api/v1/integration/uploads")
    public ResponseEntity<FeeUploadResult> upload(
            @RequestParam UUID institutionId,
            @RequestParam UUID uploadedBy,
            @RequestPart MultipartFile file) {
        try {
            FeeUploadResult result = feeUploadService.submit(
                    institutionId, uploadedBy, file.getOriginalFilename(), file.getInputStream());
            return ResponseEntity.ok(result);
        } catch (IOException e) {
            throw new UncheckedIOException("Could not read uploaded file", e);
        }
    }
}
