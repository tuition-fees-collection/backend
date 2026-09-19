/*  Data access layer */

package com.tuitionnetwork.tuitionnetwork.billing.infra;

import com.tuitionnetwork.tuitionnetwork.billing.domain.Fee;
import com.tuitionnetwork.tuitionnetwork.billing.domain.FeeStatus;
import com.tuitionnetwork.tuitionnetwork.billing.domain.FeeType;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;


public interface FeeRepository extends JpaRepository<Fee,UUID> {

   
    List<Fee> findByStudentId(UUID studentId);

   
    List<Fee> findByStudentIdAndStatus(
        UUID studentId,
        FeeStatus status
    );

    
    List<Fee> findByInstitutionId(UUID institutionId);

    
    List<Fee> findByInstitutionIdAndStatus(
        UUID institutionId,
        FeeStatus status
    );

    /*get outstnding and partially fees for a student */
    @Query("""
            SELECT f
            FROM Fee f
            WHERE f.studentId=:studentId
            AND f.status IN(
              com.tuitionnetwork.tuitionnetwork.billing.domain.FeeStatus.OUTSTANDING,
              com.tuitionnetwork.tuitionnetwork.billing.domain.FeeStatus.PARTIALLY_PAID
            )
            """)

    List<Fee> findPayableFeeByStudentId(
        @Param("studentId") UUID studentId
    );

    /*Get all payable fees for a list of students.
     *
     * Used when displaying a guardian's consolidated fees. */

    @Query("""
            SELECT f
            FROM Fee f
            Where f.studentId IN :studentIds
            AND f.status IN(
               com.tuitionnetwork.tuitionnetwork.billing.domain.FeeStatus.OUTSTANDING,
               com.tuitionnetwork.tuitionnetwork.billing.domain.FeeStatus.PARTIALLY_PAID
            )
            """)

            List<Fee> findPayableFeesByStudentIds(
                @Param("studentIds") List<UUID> studentIds
            );

            /*get all fees for a batch of students (balance aggregation) */
            /*gurdian has more than one child so use it insted of individual student queries */
            List<Fee> findByStudentIdIn(List<UUID> studentIds);

            /*get all fees created from a specific CSV upload batch */
            List<Fee> findBySourceBatchId(UUID sourceBatchId);

            /* get a student's fees belonging to a specific institution */
            List<Fee> findByStudentIdAndInstitutionId(
            UUID studentId,
            UUID institutionId
            );



            @Query("""
                         SELECT f
                         FROM Fee f
                         WHERE f.institutionId = :institutionId
                         AND f.status = :status
                         AND (:feeType IS NULL OR f.feeType = :feeType)
                         AND (:collectionPeriod IS NULL OR f.collectionPeriod = :collectionPeriod)
                         ORDER BY f.studentId, f.feeType
                    """)
            List<Fee> findInstitutionCollections(
                       @Param("institutionId") UUID institutionId,
                       @Param("status") FeeStatus status,
                       @Param("feeType") FeeType feeType,
                       @Param("collectionPeriod") String collectionPeriod
                       );

            @Query("""
                         SELECT f
                         FROM Fee f
                         WHERE f.status = :status
                         AND (:feeType IS NULL OR f.feeType = :feeType)
                         AND (:collectionPeriod IS NULL OR f.collectionPeriod = :collectionPeriod)
                         ORDER BY f.studentId, f.feeType
                    """)
            List<Fee> findNetworkCollections(
                       @Param("status") FeeStatus status,
                       @Param("feeType") FeeType feeType,
                       @Param("collectionPeriod") String collectionPeriod
                       );


}
    
        
