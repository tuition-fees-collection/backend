package com.tuitionnetwork.tuitionnetwork.payment.port.portout;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
public interface Backofficegatewayport {
    Optional<CustomerProfile> lookupCustomer(String nationalId);
    CardGatewayPort.Result chargeSource(SourceChargeCommand command, String idempotencyKey);
    CardGatewayPort.Result getSourcePaymentStatus(String gatewayPaymentId);
    record CustomerProfile(
            String customerId,
            String fullName,
            String status,
            List<Account> accounts,
            List<Card> cards
    ) {
        public boolean holds(String sourceId) {
            return accounts.stream().anyMatch(account -> account.accountId().equals(sourceId))
                    || cards.stream().anyMatch(card -> card.cardId().equals(sourceId));
        }

        public boolean isActive() {
            return "ACTIVE".equals(status);
        }
    }
    record Account(
            String accountId,
            String accountNumber,
            String type,
            String currency,
            BigDecimal availableBalance,
            String status
    ) {
        public boolean isUsable() {
            return "ACTIVE".equals(status);
        }
    }
    record Card(
            String cardId,
            String maskedNumber,
            String scheme,
            String type,
            String holderName,
            String expiry,
            String status,
            BigDecimal availableLimit,
            String linkedAccountId
    ) {
        public boolean isUsable() {
            return "ACTIVE".equals(status);
        }
        public boolean isEppEligible() {
            return "CREDIT".equals(type);
        }
    }
    record SourceChargeCommand(
            UUID paymentId,
            String sourceId,
            BigDecimal amount,
            String currency,
            String reference,
            String description,
            boolean capture
    ) {
    }
}