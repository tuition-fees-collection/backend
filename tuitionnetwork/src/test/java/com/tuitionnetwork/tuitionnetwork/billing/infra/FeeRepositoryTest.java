package com.tuitionnetwork.tuitionnetwork.billing.infra;

import com.tuitionnetwork.tuitionnetwork.billing.domain.Fee;
import com.tuitionnetwork.tuitionnetwork.billing.domain.FeeType;
import com.tuitionnetwork.tuitionnetwork.billing.domain.FeeStatus;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.List;
import java.util.UUID;
import java.math.BigDecimal;
import static org.junit.jupiter.api.Assertions.*;


@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)

 class FeeRepositoryTest {

    @Autowired
    private FeeRepository feeRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private UUID studentId;
    private UUID institutionId;
    private UUID otherStudentId;

    private void seedStudent(UUID studentId, String nationalIdHmac){
        jdbcTemplate.update(
            "INSERT INTO identity.student (id, national_id_hmac, full_name) VALUES (?, ?, ?)",
            studentId, nationalIdHmac, "Test Student " + studentId);
    }

    private void seedInstitution(UUID institutionId){
        jdbcTemplate.update(
            "INSERT INTO billing.institution (id, name, type, status) VALUES (?, ?, ?, ?)",
            institutionId, "Test Institution", "SCHOOL", "ACTIVE");
    }

    @BeforeEach
    void setUp(){

        studentId=UUID.randomUUID();
        institutionId=UUID.randomUUID();
        otherStudentId=UUID.randomUUID();
        seedStudent(studentId, "hmac-" + studentId);
        seedStudent(otherStudentId, "hmac-" + otherStudentId);
        seedInstitution(institutionId);
         Fee fee1=new Fee(
            studentId,
            institutionId,
            FeeType.TUITION,
            "2026-2027",
            "EGP",
            new BigDecimal("10000.00"),
            null

         );

         Fee fee2=new Fee(
            studentId,
            institutionId,
            FeeType.BUS,
            "2026-2027",
            "EGP",
            new BigDecimal("3000.00"),
            null
         );

            Fee fee3=new Fee(
                otherStudentId,
                institutionId,
                FeeType.TUITION,
                "2026-2027",
                "EGP",
                new BigDecimal("5000.00"),
                null
            );

            fee1.applyPayment(new BigDecimal("2000.00")); // PARTIAL
            fee3.applyPayment(new BigDecimal("5000.00")); // PAID

            feeRepository.saveAll(List.of(fee1,fee2,fee3));
    }

    @Test
    void testFindStudentById(){
        List<Fee> result=feeRepository.findByStudentId(studentId);
        assertEquals(2,result.size());
        assertTrue(result.stream().allMatch(fee->fee.getStudentId().equals(studentId)));
    }

    @Test 
    void testFindByStudentIdAndStatus(){
        List<Fee> result=feeRepository.findByStudentIdAndStatus(studentId,FeeStatus.PARTIAL);
        assertEquals(1,result.size());
        assertEquals(FeeStatus.PARTIAL,result.get(0).getStatus());
        assertEquals(studentId,result.get(0).getStudentId());
    }

    @Test 
    void testFindByInstitutionId(){
        List<Fee> result=feeRepository.findByInstitutionId(institutionId);
        assertEquals(3,result.size());
        assertTrue(result.stream().allMatch(fee->fee.getInstitutionId().equals(institutionId)));
    
    }

    @Test 
    void testFindByInstitutionIdAndStatus(){
        List<Fee> result=feeRepository.findByInstitutionIdAndStatus(institutionId,FeeStatus.PAID);
        assertEquals(1,result.size());
        assertEquals(FeeStatus.PAID,result.get(0).getStatus());
        assertEquals(institutionId,result.get(0).getInstitutionId());
    }

    @Test
    void testFindpayableFeesByStudentId(){
        List<Fee> result=feeRepository.findPayableFeeByStudentId(studentId);
        assertEquals(2,result.size());
        assertTrue(result.stream().allMatch(fee->fee.getStudentId().equals(studentId)));
        assertTrue(result.stream().allMatch(fee->fee.getStatus()==FeeStatus.OUTSTANDING || fee.getStatus()==FeeStatus.PARTIAL));
    }

    @Test 
    void testFindPayableFeesByStudentIds(){
        List<Fee> result=feeRepository.findPayableFeesByStudentIds(
            List.of(studentId,otherStudentId));
        assertEquals(2,result.size());
        assertTrue(result.stream().allMatch(fee->fee.getStudentId().equals(studentId) || fee.getStudentId().equals(otherStudentId)));
        assertTrue(result.stream().allMatch(fee->fee.getStatus()==FeeStatus.OUTSTANDING || fee.getStatus()==FeeStatus.PARTIAL));
    }

    @Test 
    void testFindByStudentIdIn(){
        List<Fee> result=feeRepository.findByStudentIdIn(List.of(studentId,otherStudentId));
        assertEquals(3,result.size());
        
    }

    @Test 
    void testFindByIdForUpdate(){
        Fee savedFee=feeRepository.findByStudentId(studentId).get(0);
        Fee result=feeRepository.findByIdForUpdate(savedFee.getId()).orElseThrow();
        assertEquals(savedFee.getId(),result.getId());
    }


}
