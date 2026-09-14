package com.tuitionnetwork.tuitionnetwork.identity.infra.ports;

import com.tuitionnetwork.tuitionnetwork.identity.infra.ports.bankmock.MoiHolderResponse;
import com.tuitionnetwork.tuitionnetwork.identity.infra.ports.bankmock.MoiNameMatchResponse;
import com.tuitionnetwork.tuitionnetwork.identity.infra.ports.bankmock.MoiValidateRequest;
import com.tuitionnetwork.tuitionnetwork.identity.infra.ports.bankmock.MoiValidationResponse;
import com.tuitionnetwork.tuitionnetwork.payments.infra.ports.bankmock.BankMockApiException;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.util.List;
import java.util.Map;

@Component
public class MoiVerificationAdapter implements MoiVerificationPort {

    private final RestClient bankMockRestClient;

    public MoiVerificationAdapter(RestClient bankMockRestClient) {
        this.bankMockRestClient = bankMockRestClient;
    }

    @Override
    public VerificationResult verify(VerificationCommand command) {
        try {
            MoiValidationResponse response = bankMockRestClient.post()
                    .uri("/api/v1/moi/validate")
                    .body(new MoiValidateRequest(command.nationalId(), command.fullName(), command.purpose()))
                    .retrieve()
                    .body(MoiValidationResponse.class);
            return mapRequired(response);
        } catch (BankMockApiException e) {
            throw translate(e);
        } catch (RestClientException e) {
            throw unavailable(e);
        }
    }

    @Override
    public VerificationResult findByVerificationId(String verificationId) {
        if (verificationId == null || verificationId.isBlank()) {
            throw new IllegalArgumentException("Verification ID is required");
        }
        try {
            MoiValidationResponse response = bankMockRestClient.get()
                    .uri("/api/v1/moi/verifications/{id}", verificationId)
                    .retrieve()
                    .body(MoiValidationResponse.class);
            return mapRequired(response);
        } catch (BankMockApiException e) {
            throw translate(e);
        } catch (RestClientException e) {
            throw unavailable(e);
        }
    }

    private VerificationResult mapRequired(MoiValidationResponse response) {
        if (response == null) {
            throw new MoiVerificationException(
                    "EMPTY_RESPONSE", "MOI service returned an empty response", Map.of(), null);
        }

        MoiHolderResponse holder = response.holder();
        Holder mappedHolder = holder == null ? null : new Holder(
                holder.fullNameEnglish(), holder.fullNameArabic(), holder.birthDate(), holder.age(),
                holder.gender(), holder.governorate(), holder.governorateCode(), holder.checksumValid());

        MoiNameMatchResponse nameMatch = response.nameMatch();
        NameMatch mappedNameMatch = nameMatch == null ? null : new NameMatch(
                nameMatch.provided(), nameMatch.matched(), nameMatch.score(), nameMatch.note());

        return new VerificationResult(
                response.verificationId(), response.valid(), response.nationalId(), response.recordStatus(),
                mappedHolder, mappedNameMatch,
                response.eligibility() == null ? Map.of() : Map.copyOf(response.eligibility()),
                response.reasons() == null ? List.of() : List.copyOf(response.reasons()),
                response.purpose(), response.verifiedAt(), response.createdAt());
    }

    private MoiVerificationException translate(BankMockApiException e) {
        return new MoiVerificationException(e.code(), e.getMessage(), e.details(), e);
    }

    private MoiVerificationException unavailable(RestClientException e) {
        return new MoiVerificationException(
                "MOI_UNAVAILABLE", "MOI service is unavailable", Map.of(), e);
    }
}
