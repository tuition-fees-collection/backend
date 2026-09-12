package com.tuitionnetwork.tuitionnetwork.identity.infra.security;

import com.tuitionnetwork.tuitionnetwork.identity.domain.NationalId;
import org.junit.jupiter.api.Test;

import java.util.Base64;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class HmacSha256NationalIdHasherTest {

    private static final String TEST_KEY =
            "YWJjZGVmZ2hpamtsbW5vcHFyc3R1dnd4eXpBQkNERUY=";

    private final HmacSha256NationalIdHasher hasher = new HmacSha256NationalIdHasher(TEST_KEY);

    @Test
    void producesStableLowercaseHexWithoutReturningPlaintext() {
        NationalId nationalId = new NationalId("29801011234567");

        String first = hasher.hash(nationalId);
        String second = hasher.hash(nationalId);

        assertThat(first).isEqualTo(second).matches("[0-9a-f]{64}");
        assertThat(first).doesNotContain(nationalId.value());
    }

    @Test
    void differentIdsAndKeysProduceDifferentHashes() {
        var otherKeyHasher = new HmacSha256NationalIdHasher(
                Base64.getEncoder().encodeToString(new byte[32]));

        assertThat(hasher.hash(new NationalId("29801011234567")))
                .isNotEqualTo(hasher.hash(new NationalId("29801011234568")))
                .isNotEqualTo(otherKeyHasher.hash(new NationalId("29801011234567")));
    }

    @Test
    void rejectsInvalidOrShortKeys() {
        assertThatThrownBy(() -> new HmacSha256NationalIdHasher("not-base64!"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("valid Base64");
        assertThatThrownBy(() -> new HmacSha256NationalIdHasher(
                Base64.getEncoder().encodeToString(new byte[31])))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("256 bits");
    }
}
