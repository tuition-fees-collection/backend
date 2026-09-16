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
import org.springframework.test.context.ActiveProfiles;

import java.util.List;
import java.util.UUID;
import java.math.BigDecimal;
import static org.junit.jupiter.api.Assertions.*;


@ActiveProfiles("test")
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
            FeeType.TRANSPORTATION,
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
        List<Fee> result=feeRepository.findByStudentIdAndStatus(studentId,FeeStatus.PARTIALLY_PAID);
        assertEquals(1,result.size());
        assertEquals(FeeStatus.PARTIALLY_PAID,result.get(0).getStatus());
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
        List<Fee> result=feeRepository.findByInstitutionIdAndStatus(institutionId,FeeStatus.SETTLED);
        assertEquals(1,result.size());
        assertEquals(FeeStatus.SETTLED,result.get(0).getStatus());
        assertEquals(institutionId,result.get(0).getInstitutionId());
    }

    @Test
    void testFindpayableFeesByStudentId(){
        List<Fee> result=feeRepository.findPayableFeeByStudentId(studentId);
        assertEquals(2,result.size());
        assertTrue(result.stream().allMatch(fee->fee.getStudentId().equals(studentId)));
        assertTrue(result.stream().allMatch(fee->fee.getStatus()==FeeStatus.OUTSTANDING || fee.getStatus()==FeeStatus.PARTIALLY_PAID));
    }

    @Test 
    void testFindPayableFeesByStudentIds(){
        List<Fee> result=feeRepository.findPayableFeesByStudentIds(
            List.of(studentId,otherStudentId));
        assertEquals(2,result.size());
        assertTrue(result.stream().allMatch(fee->fee.getStudentId().equals(studentId) || fee.getStudentId().equals(otherStudentId)));
        assertTrue(result.stream().allMatch(fee->fee.getStatus()==FeeStatus.OUTSTANDING || fee.getStatus()==FeeStatus.PARTIALLY_PAID));
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

    @Test 
    void testFindInstitutionCollections_Outstanding(){
        List<Fee> result=feeRepository.findInstitutionCollections(institutionId, FeeStatus.OUTSTANDING, null, null);
        assertEquals(1, result.size());
        assertEquals(FeeStatus.OUTSTANDING, result.get(0).getStatus());
        assertEquals(institutionId, result.get(0).getInstitutionId());
    }

    @Test 
    void testFindInstitutionCollections_PartiallyPaid(){
        List<Fee> result=feeRepository.findInstitutionCollections(institutionId, FeeStatus.PARTIALLY_PAID, null, null);
        assertEquals(1, result.size());
        assertEquals(FeeStatus.PARTIALLY_PAID, result.get(0).getStatus());
        assertEquals(institutionId, result.get(0).getInstitutionId());
    }

    @Test 
    void testFindInstitutionCollections_Settled(){
        List<Fee> result=feeRepository.findInstitutionCollections(institutionId, FeeStatus.SETTLED, null, null);
        assertEquals(1, result.size());
        assertEquals(FeeStatus.SETTLED, result.get(0).getStatus());
        assertEquals(institutionId, result.get(0).getInstitutionId());
    }

    @Test 
    void testFindInstitutionCollections_ByFeeType(){
        List<Fee> result=feeRepository.findInstitutionCollections(institutionId, FeeStatus.PARTIALLY_PAID, FeeType.TUITION, null);
        assertEquals(1, result.size());
        assertEquals(FeeType.TUITION, result.get(0).getFeeType());
        assertEquals(FeeStatus.PARTIALLY_PAID, result.get(0).getStatus());
    }

     @Test 
    void testFindInstitutionCollections_ByNonMatchingFeeType(){
        List<Fee> result=feeRepository.findInstitutionCollections(institutionId, FeeStatus.PARTIALLY_PAID, FeeType.TRANSPORTATION, null);
        assertTrue(result.isEmpty());
    }

     @Test 
    void testFindInstitutionCollections_ByCollectionPeriod(){
        List<Fee> result=feeRepository.findInstitutionCollections(institutionId, FeeStatus.OUTSTANDING, null, "2026-2027");
        assertEquals(1, result.size());
        assertEquals("2026-2027", result.get(0).getCollectionPeriod());
       
    }

     @Test 
    void testFindInstitutionCollections_ByNonMatchingPeriod(){
        List<Fee> result=feeRepository.findInstitutionCollections(institutionId, FeeStatus.OUTSTANDING, null, "2025-2026");
        assertTrue(result.isEmpty());
    }

    @Test
    void testFindInstitutionCollections_DoesNotReturnOtherInstitutionFees() {

               UUID otherInstitutionId = UUID.randomUUID();
              seedInstitution(otherInstitutionId);
               Fee otherInstitutionFee = new Fee(
                      otherStudentId,
                      otherInstitutionId,
                      FeeType.TRANSPORTATION,
            "2026-2027",
            "EGP",
                      new BigDecimal("4000.00"),
             null
                     );

                feeRepository.saveAndFlush(otherInstitutionFee);

              List<Fee> result = feeRepository.findInstitutionCollections(
                     institutionId,
                     FeeStatus.OUTSTANDING,
             null,
             null
                   );

                 assertEquals(1, result.size());

                assertTrue(result.stream().
                           allMatch(fee ->
                            fee.getInstitutionId().equals(institutionId))
                          );

               assertTrue(result.stream().
                            noneMatch(fee ->
                            fee.getInstitutionId().equals(otherInstitutionId))
                          );
}



    



    


}
