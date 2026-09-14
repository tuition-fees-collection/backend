package com.tuitionnetwork.tuitionnetwork.identity.infra.ports;

import org.junit.jupiter.api.Test;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class MoiVerificationAdapterTest {

    @Test
    void unreachableMoiServiceIsMappedToStableException() {
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(Duration.ofMillis(100));
        requestFactory.setReadTimeout(Duration.ofMillis(100));

        MoiVerificationAdapter adapter = new MoiVerificationAdapter(RestClient.builder()
                .baseUrl("http://127.0.0.1:1")
                .requestFactory(requestFactory)
                .build());

        assertThatThrownBy(() -> adapter.verify(new MoiVerificationPort.VerificationCommand(
                "29805150101023", "Mona Samir Abdelrahman", "ACCOUNT_OPENING")))
                .isInstanceOf(MoiVerificationException.class)
                .satisfies(error -> {
                    MoiVerificationException exception = (MoiVerificationException) error;
                    assertThat(exception.code()).isEqualTo("MOI_UNAVAILABLE");
                    assertThat(exception.getMessage()).isEqualTo("MOI service is unavailable");
                });
    }
}
