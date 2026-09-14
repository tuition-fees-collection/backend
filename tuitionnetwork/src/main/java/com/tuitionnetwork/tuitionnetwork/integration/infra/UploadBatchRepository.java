package com.tuitionnetwork.tuitionnetwork.integration.infra;

import com.tuitionnetwork.tuitionnetwork.integration.domain.UploadBatch;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface UploadBatchRepository extends JpaRepository<UploadBatch, UUID> {

    List<UploadBatch> findByInstitutionIdOrderByUploadedAtDesc(UUID institutionId);

    Optional<UploadBatch> findFirstByInstitutionIdAndFileChecksum(UUID institutionId, String fileChecksum);

    Optional<UploadBatch> findFirstByInstitutionIdAndExternalBatchReference(
            UUID institutionId, String externalBatchReference);
}
