package com.tuitionnetwork.tuitionnetwork.identity.infra.security;

import com.tuitionnetwork.tuitionnetwork.identity.domain.NationalId;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class AesGcmNationalIdEncryptorTest {

    private static final String TEST_KEY =
            "MDEyMzQ1Njc4OWFiY2RlZjAxMjM0NTY3ODlhYmNkZWY=";

    @Test
    void ciphertextDoesNotContainPlaintextAndUsesANewIvEachTime() {
        var encryptor = new AesGcmNationalIdEncryptor(TEST_KEY, new java.security.SecureRandom());
        var nationalId = new NationalId("29801011234567");

        byte[] first = encryptor.encrypt(nationalId);
        byte[] second = encryptor.encrypt(nationalId);

        assertThat(new String(first, StandardCharsets.UTF_8)).doesNotContain(nationalId.value());
        assertThat(second).isNotEqualTo(first);
        assertThat(first[0]).isEqualTo((byte) 1);
    }

    @Test
    void rejectsKeysThatAreNotExactly256Bits() {
        assertThatThrownBy(() -> new AesGcmNationalIdEncryptor("dG9vLXNob3J0", new java.security.SecureRandom()))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("exactly 32 bytes");
    }
}
