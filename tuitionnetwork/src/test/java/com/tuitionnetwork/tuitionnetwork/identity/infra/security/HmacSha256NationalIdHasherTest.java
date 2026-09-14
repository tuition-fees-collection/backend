package com.tuitionnetwork.tuitionnetwork.identity.infra.security;

import com.tuitionnetwork.tuitionnetwork.identity.domain.NationalId;
import org.junit.jupiter.api.Test;

import java.util.Base64;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class HmacSha256NationalIdHasherTest {

    private static final String TEST_KEY =
            "YWJjZGVmZ2hpamtsbW5vcHFyc3R1dnd4eXpBQkNERUY=";

    private final HmacSha256NationalIdHasher hasher = new HmacSha256NationalIdHasher(TEST_KEY);

    @Test
    void sameNationalIdProducesSameHashWithoutReturningPlaintext() {
        var nationalId = new NationalId("29801011234567");

        String first = hasher.hash(nationalId);
        String second = hasher.hash(nationalId);

        assertEquals(first, second);
        assertTrue(first.matches("[0-9a-f]{64}"));
        assertFalse(first.contains(nationalId.value()));
    }

    @Test
    void differentNationalIdsProduceDifferentHashes() {
        assertNotEquals(
                hasher.hash(new NationalId("29801011234567")),
                hasher.hash(new NationalId("29801011234568")));
    }

    @Test
    void differentKeysProduceDifferentHashes() {
        var other = new HmacSha256NationalIdHasher(
                Base64.getEncoder().encodeToString(new byte[32]));
        var nationalId = new NationalId("29801011234567");

        assertNotEquals(hasher.hash(nationalId), other.hash(nationalId));
    }

    @Test
    void shortKeyIsRejected() {
        assertThrows(
                IllegalStateException.class,
                () -> new HmacSha256NationalIdHasher(
                        Base64.getEncoder().encodeToString(new byte[31])));
    }

    @Test
    void invalidBase64IsRejected() {
        assertThrows(
                IllegalStateException.class,
                () -> new HmacSha256NationalIdHasher("not-base64!"));
    }

    @Test
    void nullNationalIdIsRejected() {
        assertThrows(NullPointerException.class, () -> hasher.hash(null));
    }
}
