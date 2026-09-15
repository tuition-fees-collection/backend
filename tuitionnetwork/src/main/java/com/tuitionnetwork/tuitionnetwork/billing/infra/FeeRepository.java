/*  Data access layer */

package com.tuitionnetwork.tuitionnetwork.billing.infra;

import com.tuitionnetwork.tuitionnetwork.billing.domain.Fee;
import com.tuitionnetwork.tuitionnetwork.billing.domain.FeeStatus;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;


public interface FeeRepository extends JpaRepository<Fee,UUID> {

    /*get all fees for a specific student */
    List<Fee> findByStudentId(UUID studentId);

    /* get all fees for a student with specific status */
    List<Fee> findByStudentIdAndStatus(
        UUID studentId,
        FeeStatus status
    );

    /*get all fees belonging to specific institution */
    List<Fee> findByInstitutionId(UUID institutionId);

    /*get all fees for an institution with  specific status */
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

            /*lock a fee while processing a payment */
            @Lock(LockModeType.PESSIMISTIC_WRITE)
                @Query("""
                    SELECT f
                    FROM Fee f
                    WHERE f.id=:feeId   
                    """)
                    Optional<Fee> findByIdForUpdate(
                        @Param("feeId") UUID feeId
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


}
    
        
