package com.tuitionnetwork.tuitionnetwork.identity.infra.ports;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;


public interface MoiVerificationPort {

    VerificationResult verify(VerificationCommand command);

    VerificationResult findByVerificationId(String verificationId);

    record VerificationCommand(String nationalId, String fullName, String purpose) {
        public VerificationCommand {
            if (nationalId == null || !nationalId.matches("\\d{14}")) {
                throw new IllegalArgumentException("National ID must contain exactly 14 digits");
            }
        }
    }

    record VerificationResult(
            String verificationId,
            boolean valid,
            String nationalId,
            String recordStatus,
            Holder holder,
            NameMatch nameMatch,
            Map<String, Object> eligibility,
            List<String> reasons,
            String purpose,
            OffsetDateTime verifiedAt,
            OffsetDateTime createdAt) {
    }

    record Holder(
            String fullNameEnglish,
            String fullNameArabic,
            LocalDate birthDate,
            Integer age,
            String gender,
            String governorate,
            String governorateCode,
            Boolean checksumValid) {
    }

    record NameMatch(
            String provided,
            Boolean matched,
            BigDecimal score,
            String note) {
    }
}
