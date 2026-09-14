package com.tuitionnetwork.tuitionnetwork.identity.infra;

import com.tuitionnetwork.tuitionnetwork.identity.domain.NationalId;
import com.tuitionnetwork.tuitionnetwork.identity.infra.repository.StudentIdentityLookupRepository;
import com.tuitionnetwork.tuitionnetwork.identity.service.NationalIdHasher;
import com.tuitionnetwork.tuitionnetwork.identity.service.StudentNotFoundException;
import com.tuitionnetwork.tuitionnetwork.integration.infra.ports.StudentResolutionPort;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
public class StudentResolutionAdapter implements StudentResolutionPort {

    private final ObjectProvider<NationalIdHasher> nationalIdHasherProvider;
    private final StudentIdentityLookupRepository studentIdentityLookupRepository;

    public StudentResolutionAdapter(
            ObjectProvider<NationalIdHasher> nationalIdHasherProvider,
            StudentIdentityLookupRepository studentIdentityLookupRepository) {
        this.nationalIdHasherProvider = nationalIdHasherProvider;
        this.studentIdentityLookupRepository = studentIdentityLookupRepository;
    }

    @Override
    public boolean isAvailable() {
        return nationalIdHasherProvider.getIfAvailable() != null;
    }

    @Override
    public UUID resolveRequiredStudentId(String nationalId) {
        NationalIdHasher nationalIdHasher = nationalIdHasherProvider.getIfAvailable();
        if (nationalIdHasher == null) {
            throw new IllegalStateException("National ID hashing is not configured");
        }
        String nationalIdHmac = nationalIdHasher.hash(new NationalId(nationalId));
        return studentIdentityLookupRepository.findStudentIdByNationalIdHmac(nationalIdHmac)
                .orElseThrow(StudentNotFoundException::new);
    }
}
