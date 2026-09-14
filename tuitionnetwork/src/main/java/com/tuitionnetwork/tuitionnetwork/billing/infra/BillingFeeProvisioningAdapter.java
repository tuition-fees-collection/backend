package com.tuitionnetwork.tuitionnetwork.billing.infra;

import com.tuitionnetwork.tuitionnetwork.billing.domain.Fee;
import com.tuitionnetwork.tuitionnetwork.billing.domain.FeeType;
import com.tuitionnetwork.tuitionnetwork.integration.infra.ports.FeeProvisioningPort;
import com.tuitionnetwork.tuitionnetwork.integration.infra.ports.StudentResolutionPort;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.UUID;

@Component
public class BillingFeeProvisioningAdapter implements FeeProvisioningPort {

    private final FeeRepository feeRepository;
    private final Optional<StudentResolutionPort> studentResolutionPort;

    public BillingFeeProvisioningAdapter(
            FeeRepository feeRepository,
            Optional<StudentResolutionPort> studentResolutionPort) {
        this.feeRepository = feeRepository;
        this.studentResolutionPort = studentResolutionPort;
    }

    @Override
    public boolean isAvailable() {
        return studentResolutionPort.filter(StudentResolutionPort::isAvailable).isPresent();
    }

    @Override
    public UUID provisionFee(Command command) {
        StudentResolutionPort resolver = studentResolutionPort
                .filter(StudentResolutionPort::isAvailable)
                .orElseThrow(
                () -> new IllegalStateException("Student identity resolution is not configured"));
        UUID studentId = resolver.resolveRequiredStudentId(command.studentNationalId());
        Fee fee = new Fee(
                studentId,
                command.institutionId(),
                FeeType.valueOf(command.feeType()),
                command.collectionPeriod(),
                command.currency(),
                command.amount(),
                command.sourceBatchId());

        return feeRepository.save(fee).getId();
    }
}
