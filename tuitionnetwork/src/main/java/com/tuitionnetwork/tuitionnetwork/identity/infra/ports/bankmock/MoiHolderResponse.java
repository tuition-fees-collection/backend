package com.tuitionnetwork.tuitionnetwork.identity.infra.ports.bankmock;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.LocalDate;

public record MoiHolderResponse(
        @JsonProperty("full_name_en") String fullNameEnglish,
        @JsonProperty("full_name_ar") String fullNameArabic,
        @JsonProperty("birth_date") LocalDate birthDate,
        Integer age,
        String gender,
        String governorate,
        @JsonProperty("governorate_code") String governorateCode,
        @JsonProperty("checksum_valid") Boolean checksumValid) {
}
