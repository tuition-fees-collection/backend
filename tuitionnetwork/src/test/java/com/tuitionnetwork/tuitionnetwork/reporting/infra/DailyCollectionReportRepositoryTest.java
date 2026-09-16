package com.tuitionnetwork.tuitionnetwork.reporting.infra;

import com.tuitionnetwork.tuitionnetwork.billing.domain.Fee;
import com.tuitionnetwork.tuitionnetwork.billing.domain.FeeType;
import com.tuitionnetwork.tuitionnetwork.billing.infra.FeeRepository;
import com.tuitionnetwork.tuitionnetwork.payment.domain.Payment;
import com.tuitionnetwork.tuitionnetwork.payment.domain.PaymentAllocation;
import com.tuitionnetwork.tuitionnetwork.payment.domain.PaymentMethod;
import com.tuitionnetwork.tuitionnetwork.payment.domain.PaymentType;
import com.tuitionnetwork.tuitionnetwork.payment.infra.persistence.repository.PaymentAllocationJpaRepository;
import com.tuitionnetwork.tuitionnetwork.payment.infra.persistence.repository.PaymentJpaRepository;
import com.tuitionnetwork.tuitionnetwork.reporting.domain.DailyCollectionRecord;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@ActiveProfiles("test")
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)

 class DailyCollectionReportRepositoryTest {
    @Autowired
    private DailyCollectionReportRepository reportRepository;
    @Autowired 
    private FeeRepository feeRepository;
    @Autowired 
    private PaymentJpaRepository paymentRepository;
    @Autowired 
    private PaymentAllocationJpaRepository allocationRepository;
    @Autowired 
    private JdbcTemplate jdbcTemplate;

    private UUID studentId;
    private UUID institutionId;
    private UUID guardianId;
    private UUID officerId;


    @BeforeEach 
    void setUp(){
        studentId=UUID.randomUUID();
        institutionId=UUID.randomUUID();
        guardianId=UUID.randomUUID();
        officerId=UUID.randomUUID();

        seedStudent(studentId);
        seedInstitution(institutionId);
        seedGuardian(guardianId);
        seedBackofficeEmployee(officerId);
    }
    private void seedStudent(UUID id){
         jdbcTemplate.update("""
                INSERT INTO identity.student
                    (id, national_id_hmac, full_name)
                     VALUES (?, ?, ?)
                     """,
                     id,
                     "hmac-" + id,
                    "Reporting Test Student"
                  );
    }
    private void seedInstitution(UUID id){
        jdbcTemplate.update("""
                INSERT INTO billing.institution
                    (id, name, type,status)
                     VALUES (?, ?, ?,?)
                     """,
                     id,
                     "Reporting Test Institution",
                     "SCHOOL",
                     "ACTIVE"
                  );

    }

     private void seedGuardian(UUID id){
        jdbcTemplate.update("""
                INSERT INTO identity.guardian
                    (id,national_id_hmac, national_id_encrypted, full_name, verified)
                     VALUES (?, ?, ?,?,?)
                     """,
                     id,
                     "guardian-hmac-" + id,
                     ("encrypted-" + id).getBytes(),
                     "Reporting Test Guardian",
                     true
                  );

    }

    private void seedBackofficeEmployee(UUID id){
          UUID roleId = jdbcTemplate.queryForObject("""
                  SELECT id
                  FROM identity.role
                  WHERE name = ?
                 """,
                  UUID.class,
                 "BANK_OFFICER"
                );
        jdbcTemplate.update("""
                INSERT INTO identity.backoffice_employee
                    (id, full_name, branch, role_id)
                     VALUES (?, ?,?,?)
                     """,
                     id,
                     "Reporting Test Bank Officer",
                     "Test Branch",
                     roleId
                  );

    }

    private Fee createFee(
        UUID studentId,
        UUID institutionId,
        FeeType feeType,
        BigDecimal amount
    ){
        Fee fee=new Fee(studentId, institutionId, feeType,  "2026-2027", "EGP", amount, null);
        return feeRepository.saveAndFlush(fee);
    }

    private Payment createPayment(BigDecimal amount){
        Payment payment = Payment.create(
                "report-" + UUID.randomUUID(),
                guardianId,
                officerId,
                PaymentMethod.CREDIT_CARD,
                PaymentType.PARTIAL,
                amount,
                "EGP"
               );

        return payment;
    }

    @Test
    void findDailyCollections_ReturnsSuccessfulPayment() {

        Fee fee = createFee(
                studentId,
                institutionId,
                FeeType.TUITION,
                new BigDecimal("10000.00")
        );

        Payment payment =
                createPayment(new BigDecimal("4000.00"));

        payment.markPending();

        payment.markSuccessful(
                "gateway-" + UUID.randomUUID(),
                new BigDecimal("4000.00"),
                "195000",
                "3355",
                "00",
                "Approved",
                "411111******1111",
                "VISA"

        );
        paymentRepository.saveAndFlush(payment);

        allocationRepository.saveAndFlush(
                PaymentAllocation.of(
                        payment.getId(),
                        fee.getId(),
                        new BigDecimal("4000.00")
                )
        );

        Instant start =Instant.now().minus(1, ChronoUnit.HOURS);

        Instant end =Instant.now().plus(1, ChronoUnit.HOURS);

        List<DailyCollectionRecord> result =reportRepository.findDailyCollections(
                        institutionId,
                        start,
                        end
                       );

        assertEquals(1, result.size());

        DailyCollectionRecord record = result.get(0);

        assertEquals(payment.getId(), record.paymentId());
        assertEquals(studentId, record.studentId());
        assertEquals(FeeType.TUITION, record.feeType());
        assertEquals(0,new BigDecimal("4000.00").compareTo(record.amountApplied()));
        assertEquals(PaymentMethod.CREDIT_CARD,record.paymentMethod());
        assertNotNull(record.settledAt()); 
        assertEquals("3355", record.paymentReference());  
      }

      @Test
      void findDailyCollections_DoesNotReturnNonSuccessfulPayment() {
              Fee fee = createFee(
                    studentId,
                    institutionId,
                    FeeType.BUS,
                new BigDecimal("3000.00")
              );

        Payment payment=createPayment(new BigDecimal("3000.00"));

        // Remains CREATED
        paymentRepository.saveAndFlush(payment);

        allocationRepository.saveAndFlush(PaymentAllocation.of(payment.getId(),fee.getId(),new BigDecimal("3000.00")));
         Instant start=Instant.now().minus(1, ChronoUnit.HOURS);

        Instant end=Instant.now().plus(1, ChronoUnit.HOURS);

        List<DailyCollectionRecord> result =reportRepository.findDailyCollections(
                        institutionId,
                        start,
                        end
                    );

        assertTrue(result.isEmpty());
    }

     @Test
    void findDailyCollections_DoesNotReturnOtherInstitutionData() {

        UUID otherInstitutionId = UUID.randomUUID();
        UUID otherStudentId = UUID.randomUUID();

        seedInstitution(otherInstitutionId);
        seedStudent(otherStudentId);

        Fee otherFee = createFee(
                otherStudentId,
                otherInstitutionId,
                FeeType.TUITION,
                new BigDecimal("5000.00")
        );

        Payment payment =
                createPayment(new BigDecimal("5000.00"));

        payment.markPending();

        payment.markSuccessful(
                "gateway-" + UUID.randomUUID(),
                new BigDecimal("5000.00"),
                "195000",
                "3355",
                "00",
                "Approved",
                "411111******1111",
                "VISA"
        );
        paymentRepository.saveAndFlush(payment);

        allocationRepository.saveAndFlush(
                PaymentAllocation.of(
                        payment.getId(),
                        otherFee.getId(),
                        new BigDecimal("5000.00")
                )
        );

        Instant start=Instant.now().minus(1, ChronoUnit.HOURS);
        Instant end =Instant.now().plus(1, ChronoUnit.HOURS);

        List<DailyCollectionRecord> result =reportRepository.findDailyCollections(
                        institutionId,
                        start,
                        end
                       );
        assertTrue(result.isEmpty());
    }

     @Test
    void findDailyCollections_DoesNotReturnPaymentOutsideDateRange() {

        Fee fee = createFee(
                studentId,
                institutionId,
                FeeType.TUITION,
                new BigDecimal("5000.00")
        );

        Payment payment=createPayment(new BigDecimal("5000.00"));
        payment.markPending();
        payment.markSuccessful(
                "gateway-" + UUID.randomUUID(),
                new BigDecimal("5000.00"),
                "195000",
                "3355",
                "00",
                "Approved",
                "411111******1111",
                "VISA"
        );
        paymentRepository.saveAndFlush(payment);

        allocationRepository.saveAndFlush(
                PaymentAllocation.of(
                        payment.getId(),
                        fee.getId(),
                        new BigDecimal("5000.00")
                )
        );

        Instant start=Instant.now().plus(1, ChronoUnit.DAYS);
        Instant end =Instant.now().plus(2, ChronoUnit.HOURS);

        List<DailyCollectionRecord> result =reportRepository.findDailyCollections(
                        institutionId,
                        start,
                        end
                       );
        assertTrue(result.isEmpty());
    }
}
