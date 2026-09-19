package com.tuitionnetwork.tuitionnetwork.billing.service;

import com.tuitionnetwork.tuitionnetwork.billing.api.dto.StudentReceiptLine;
import com.tuitionnetwork.tuitionnetwork.billing.api.dto.StudentReceiptResponse;
import com.tuitionnetwork.tuitionnetwork.billing.infra.StudentReceiptRepository;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@Transactional(readOnly = true)
public class StudentReceiptService {

    private final FeeService feeService;
    private final StudentReceiptRepository studentReceiptRepository;

    public StudentReceiptService(FeeService feeService, StudentReceiptRepository studentReceiptRepository) {
        this.feeService = feeService;
        this.studentReceiptRepository = studentReceiptRepository;
    }

    public List<StudentReceiptResponse> getStudentReceipts(UUID studentId, UUID institutionId) {
        feeService.getFeesForStudentAndInstitution(studentId, institutionId);
        return group(studentReceiptRepository.findByStudentAndInstitution(studentId, institutionId));
    }

    List<StudentReceiptResponse> group(List<StudentReceiptLine> lines) {
        Map<UUID, List<StudentReceiptLine>> grouped = new LinkedHashMap<>();
        for (StudentReceiptLine line : lines) {
            grouped.computeIfAbsent(line.paymentId(), ignored -> new ArrayList<>()).add(line);
        }
        return grouped.values().stream().map(this::toResponse).toList();
    }

    private StudentReceiptResponse toResponse(List<StudentReceiptLine> lines) {
        StudentReceiptLine first = lines.getFirst();
        return new StudentReceiptResponse(
                first.paymentId(),
                first.referenceNumber(),
                first.issuedAt(),
                first.amount(),
                first.currency(),
                first.method(),
                null,
                null,
                lines.stream()
                        .map(line -> new StudentReceiptResponse.Line(
                                line.feeId(),
                                line.feeType() == null ? null : line.feeType().name(),
                                line.collectionPeriod(),
                                line.amountApplied()
                        ))
                        .toList()
        );
    }
}
