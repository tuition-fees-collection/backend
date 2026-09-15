package com.tuitionnetwork.tuitionnetwork.payment.api;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import com.tuitionnetwork.tuitionnetwork.payment.port.portout.Backofficegatewayport.CustomerProfile;
public record CustomerLookupResponse(
        String customerName,
        boolean active,
        List<PayableSource> sources
) {
    public record PayableSource(
            String sourceId,
            String type,
            String label,
            BigDecimal available,
            String currency,
            boolean usable
    ) {
    }

    public static CustomerLookupResponse from(CustomerProfile profile) {
        List<PayableSource> sources = new ArrayList<>();

        profile.accounts().forEach(account -> sources.add(new PayableSource(
                account.accountId(),
                "ACCOUNT",
                account.type() + " " + lastFour(account.accountNumber()),
                account.availableBalance(),
                account.currency(),
                account.isUsable())));

        profile.cards().forEach(card -> sources.add(new PayableSource(
                card.cardId(),
                "CARD",
                card.scheme() + " " + card.type() + " " + card.maskedNumber(),
                card.availableLimit(),
                "EGP",
                card.isUsable())));

        return new CustomerLookupResponse(profile.fullName(), profile.isActive(), sources);
    }
    private static String lastFour(String accountNumber) {
        if (accountNumber == null || accountNumber.length() <= 4) {
            return accountNumber == null ? "" : accountNumber;
        }
        return "****" + accountNumber.substring(accountNumber.length() - 4);
    }
}
