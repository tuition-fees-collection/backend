package com.tuitionnetwork.tuitionnetwork.payment.api;
import java.math.BigDecimal;
import java.util.List;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.tuitionnetwork.tuitionnetwork.payment.port.portout.Backofficegatewayport.CustomerProfile;

public record CustomerLookupResponse(
        @JsonProperty("customer_id") String customerId,
        @JsonProperty("national_id") String nationalId,
        @JsonProperty("full_name_en") String fullNameEnglish,
        @JsonProperty("full_name_ar") String fullNameArabic,
        String mobile,
        String status,
        List<Account> accounts,
        List<Card> cards
) {
    public record Account(
            @JsonProperty("account_id") String accountId,
            @JsonProperty("account_number") String accountNumber,
            String type,
            String currency,
            @JsonProperty("available_balance") BigDecimal availableBalance,
            String status
    ) {
    }

    public record Card(
            @JsonProperty("card_id") String cardId,
            @JsonProperty("masked_number") String maskedNumber,
            String scheme,
            String type,
            @JsonProperty("holder_name") String holderName,
            String expiry,
            String status,
            @JsonProperty("credit_limit") BigDecimal creditLimit,
            @JsonProperty("available_limit") BigDecimal availableLimit,
            @JsonProperty("linked_account_id") String linkedAccountId
    ) {
    }

    public static CustomerLookupResponse from(CustomerProfile profile, String nationalId) {
        return new CustomerLookupResponse(
                profile.customerId(),
                nationalId,
                profile.fullNameEnglish(),
                profile.fullNameArabic(),
                profile.mobile(),
                profile.status(),
                profile.accounts().stream()
                        .map(account -> new Account(
                                account.accountId(), account.accountNumber(), account.type(),
                                account.currency(), account.availableBalance(), account.status()))
                        .toList(),
                profile.cards().stream()
                        .map(card -> new Card(
                                card.cardId(), card.maskedNumber(), card.scheme(), card.type(),
                                card.holderName(), card.expiry(), card.status(),
                                card.creditLimit(), card.availableLimit(), card.linkedAccountId()))
                        .toList());
    }
}