package com.tuitionnetwork.tuitionnetwork.identity.infra.ports.bankmock;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;

public record MoiValidationResponse(
        @JsonProperty("verification_id") String verificationId,
        boolean valid,
        @JsonProperty("national_id") String nationalId,
        @JsonProperty("record_status") String recordStatus,
        MoiHolderResponse holder,
        @JsonProperty("name_match") MoiNameMatchResponse nameMatch,
        Map<String, Object> eligibility,
        List<String> reasons,
        String purpose,
        @JsonProperty("verified_at") OffsetDateTime verifiedAt,
        @JsonProperty("created_at") OffsetDateTime createdAt) {
}
