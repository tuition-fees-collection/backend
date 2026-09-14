package com.tuitionnetwork.tuitionnetwork.payments.infra.ports;

import com.tuitionnetwork.tuitionnetwork.payments.infra.ports.bankmock.BankMockApiException;
import com.tuitionnetwork.tuitionnetwork.payments.infra.ports.bankmock.CustomerAccount;
import com.tuitionnetwork.tuitionnetwork.payments.infra.ports.bankmock.CustomerCard;
import com.tuitionnetwork.tuitionnetwork.payments.infra.ports.bankmock.CustomerResponse;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;


@Tag("integration")
@ActiveProfiles("test")
@SpringBootTest
class CustomerLookupAdapterIntegrationTest {

    @Autowired
    private CustomerLookupAdapter adapter;

    @Test
    void knownCustomerReturnsAccountsAndCards() {
        CustomerResponse customer = adapter.findByNationalId("29805150101023");

        assertThat(customer.customerId()).isEqualTo("cif_100001");
        assertThat(customer.fullNameEnglish()).isEqualTo("Mona Samir Abdelrahman");
        assertThat(customer.accounts().stream().map(CustomerAccount::accountId).toList())
                .contains("acc_mona_current", "acc_mona_savings");
        assertThat(customer.cards().stream().map(CustomerCard::cardId).toList())
                .contains("card_mona_visa", "card_mona_debit");
    }

    @Test
    void frozenAccountAndBlockedCardAreReflectedInStatus() {
        CustomerResponse customer = adapter.findByNationalId("29511020204536");

        assertThat(customer.accounts().get(0).status()).isEqualTo("FROZEN");
        CustomerCard blockedCard = customer.cards().stream()
                .filter(card -> card.cardId().equals("card_ahmed_visa"))
                .findFirst().orElseThrow();
        assertThat(blockedCard.status()).isEqualTo("BLOCKED");
    }

    @Test
    void unknownNationalIdIsNotFound() {
        assertThatThrownBy(() -> adapter.findByNationalId("29999999999999"))
                .isInstanceOf(BankMockApiException.class)
                .satisfies(ex -> assertThat(((BankMockApiException) ex).code()).isEqualTo("CUSTOMER_NOT_FOUND"));
    }
}
