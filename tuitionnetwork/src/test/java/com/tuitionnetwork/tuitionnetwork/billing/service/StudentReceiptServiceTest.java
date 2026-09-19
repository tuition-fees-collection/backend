package com.tuitionnetwork.tuitionnetwork.billing.service;

import com.tuitionnetwork.tuitionnetwork.billing.api.dto.StudentReceiptLine;
import com.tuitionnetwork.tuitionnetwork.billing.api.dto.StudentReceiptResponse;
import com.tuitionnetwork.tuitionnetwork.billing.domain.Fee;
import com.tuitionnetwork.tuitionnetwork.billing.domain.FeeType;
import com.tuitionnetwork.tuitionnetwork.billing.infra.StudentReceiptRepository;
import com.tuitionnetwork.tuitionnetwork.payment.domain.PaymentMethod;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class StudentReceiptServiceTest {

    private FeeService feeService;
    private StudentReceiptRepository studentReceiptRepository;
    private StudentReceiptService studentReceiptService;

    @BeforeEach
    void setUp() {
        feeService = mock(FeeService.class);
        studentReceiptRepository = mock(StudentReceiptRepository.class);
        studentReceiptService = new StudentReceiptService(feeService, studentReceiptRepository);
    }

    @Test
    void getStudentReceipts_groupsLinesByPayment() {
        UUID studentId = UUID.randomUUID();
        UUID institutionId = UUID.randomUUID();
        UUID paymentId = UUID.randomUUID();
        UUID tuitionId = UUID.randomUUID();
        UUID busId = UUID.randomUUID();
        Instant issuedAt = Instant.parse("2026-09-18T10:00:00Z");

        when(feeService.getFeesForStudentAndInstitution(studentId, institutionId))
                .thenReturn(List.of(new Fee(studentId, institutionId, FeeType.TUITION, "2026-09", "EGP", new BigDecimal("18000.00"), null)));
        when(studentReceiptRepository.findByStudentAndInstitution(studentId, institutionId)).thenReturn(List.of(
                new StudentReceiptLine(paymentId, "RCP-1", issuedAt, new BigDecimal("9500.00"), "EGP", PaymentMethod.CASH, tuitionId, FeeType.TUITION, "2026-09", new BigDecimal("8000.00")),
                new StudentReceiptLine(paymentId, "RCP-1", issuedAt, new BigDecimal("9500.00"), "EGP", PaymentMethod.CASH, busId, FeeType.TRANSPORTATION, "2026-09", new BigDecimal("1500.00"))
        ));

        List<StudentReceiptResponse> receipts = studentReceiptService.getStudentReceipts(studentId, institutionId);

        assertEquals(1, receipts.size());
        assertEquals("RCP-1", receipts.get(0).referenceNumber());
        assertEquals(2, receipts.get(0).lines().size());
        assertEquals("TUITION", receipts.get(0).lines().get(0).feeType());
        verify(feeService).getFeesForStudentAndInstitution(studentId, institutionId);
    }

    @Test
    void getStudentReceipts_whenStudentNotAtSchool_throws() {
        UUID studentId = UUID.randomUUID();
        UUID institutionId = UUID.randomUUID();
        when(feeService.getFeesForStudentAndInstitution(studentId, institutionId))
                .thenThrow(new FeeNotFoundException("Student fee record not found"));

        FeeNotFoundException exception = assertThrows(
                FeeNotFoundException.class,
                () -> studentReceiptService.getStudentReceipts(studentId, institutionId)
        );

        assertEquals("Student fee record not found", exception.getMessage());
    }
}
