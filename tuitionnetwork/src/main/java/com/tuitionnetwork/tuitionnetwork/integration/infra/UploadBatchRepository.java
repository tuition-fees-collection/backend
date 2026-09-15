package com.tuitionnetwork.tuitionnetwork.integration.infra;

import com.tuitionnetwork.tuitionnetwork.integration.domain.UploadBatch;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface UploadBatchRepository extends JpaRepository<UploadBatch, UUID> {

    List<UploadBatch> findByInstitutionIdOrderByUploadedAtDesc(UUID institutionId);

    Optional<UploadBatch> findFirstByInstitutionIdAndFileChecksum(UUID institutionId, String fileChecksum);

    Optional<UploadBatch> findFirstByInstitutionIdAndExternalBatchReference(
            UUID institutionId, String externalBatchReference);

    @Query("SELECT b FROM UploadBatch b LEFT JOIN FETCH b.errors WHERE b.id = :id")
    Optional<UploadBatch> findByIdWithErrors(@Param("id") UUID id);

    @Query("SELECT DISTINCT b FROM UploadBatch b LEFT JOIN FETCH b.errors "
            + "WHERE b.institutionId = :institutionId ORDER BY b.uploadedAt DESC")
    List<UploadBatch> findByInstitutionIdWithErrorsOrderByUploadedAtDesc(@Param("institutionId") UUID institutionId);
}
