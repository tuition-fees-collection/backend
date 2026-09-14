package com.tuitionnetwork.tuitionnetwork.payments.infra.ports;

import com.tuitionnetwork.tuitionnetwork.payments.infra.ports.bankmock.CustomerResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

@Component
public class CustomerLookupAdapter implements CustomerLookupPort {

    private final RestClient bankMockRestClient;

    public CustomerLookupAdapter(RestClient bankMockRestClient) {
        this.bankMockRestClient = bankMockRestClient;
    }

    @Override
    public CustomerResponse findByNationalId(String nationalId) {
        return bankMockRestClient.get()
                .uri(uriBuilder -> uriBuilder.path("/api/v1/customers")
                        .queryParam("national_id", nationalId)
                        .build())
                .retrieve()
                .body(CustomerResponse.class);
    }
}
