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


    @Query("""
            SELECT f
            FROM Fee f
            WHERE f.studentId=:studentId
            AND f.status IN(
              com.tuitionnetwork.tuitionnetwork.billing.domain.FeeStatus.OUTSTANDING,
              com.tuitionnetwork.tuitionnetwork.billing.domain.FeeStatus.PARTIAL
            )
            """)

    List<Fee> findPayableFeeByStudentId(
        @Param("studentId") UUID studentId
    );



    @Query("""
            SELECT f
            FROM Fee f
            Where f.studentId IN :studentIds
            AND f.status IN(
               com.tuitionnetwork.tuitionnetwork.billing.domain.FeeStatus.OUTSTANDING,
               com.tuitionnetwork.tuitionnetwork.billing.domain.FeeStatus.PARTIAL
            )
            """)

            List<Fee> findPayableFeesByStudentIds(
                @Param("studentIds") List<UUID> studentIds
            );


            @Lock(LockModeType.PESSIMISTIC_WRITE)
                @Query("""
                    SELECT f
                    FROM Fee f
                    WHERE f.id=:feeId   
                    """)
                    Optional<Fee> findByIdForUpdate(
                        @Param("feeId") UUID feeId
                    );



            List<Fee> findByStudentIdIn(List<UUID> studentIds);

            List<Fee> findBySourceBatchId(UUID sourceBatchId);
            }
    
        
