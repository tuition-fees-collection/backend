package com.tuitionnetwork.tuitionnetwork.integration.web;

import com.tuitionnetwork.tuitionnetwork.integration.domain.UploadBatch;
import com.tuitionnetwork.tuitionnetwork.integration.service.DuplicateUploadException;
import com.tuitionnetwork.tuitionnetwork.integration.service.FeeUploadService;
import com.tuitionnetwork.tuitionnetwork.integration.service.UploadValidationException;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

class FeeUploadControllerTest {

    private final FeeUploadController controller = new FeeUploadController(mock(FeeUploadService.class));

    @Test
    void duplicateSubmissionReturnsConflictAndExistingBatchDetails() {
        UploadBatch existing = UploadBatch.start(
                UUID.randomUUID(), UUID.randomUUID(), "dues.csv", "a".repeat(64), "EXT-1");
        existing.finish(1, 1);
        DuplicateUploadException duplicate = DuplicateUploadException.forExisting(existing, "FILE_CHECKSUM");

        var response = controller.duplicateUpload(duplicate);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().code()).isEqualTo("DUPLICATE_UPLOAD");
        assertThat(response.getBody().existingStatus()).isEqualTo("COMPLETED");
        assertThat(response.getBody().matchedBy()).isEqualTo("FILE_CHECKSUM");
    }

    @Test
    void fileLevelValidationFailureReturnsBadRequest() {
        var response = controller.invalidUpload(
                new UploadValidationException("INVALID_HEADERS", "Headers are invalid"));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).isEqualTo(
                new UploadValidationErrorResponse("INVALID_HEADERS", "Headers are invalid"));
    }
}
