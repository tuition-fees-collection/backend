package com.tuitionnetwork.tuitionnetwork.identity.infra.security;

import com.tuitionnetwork.tuitionnetwork.identity.domain.NationalId;
import org.junit.jupiter.api.Test;
import java.util.Base64;
import static org.junit.jupiter.api.Assertions.*;

class HmacSha256NationalIdHasherTest {
    private static final String TEST_KEY = "YWJjZGVmZ2hpamtsbW5vcHFyc3R1dnd4eXpBQkNERUY=";
    private final HmacSha256NationalIdHasher hasher = new HmacSha256NationalIdHasher(TEST_KEY);

    @Test
    void sameNationalIdProducesSameHash() {
        var id = new NationalId("29801011234567");
        assertEquals(hasher.hash(id), hasher.hash(id));
    }

    @Test
    void differentNationalIdsProduceDifferentHashes() {
        assertNotEquals(hasher.hash(new NationalId("29801011234567")),
                hasher.hash(new NationalId("29801011234568")));
    }

    @Test
    void hashIs64LowercaseHexCharacters() {
        assertTrue(hasher.hash(new NationalId("29801011234567")).matches("[0-9a-f]{64}"));
    }

    @Test
    void shortKeyIsRejected() {
        assertThrows(IllegalStateException.class,
                () -> new HmacSha256NationalIdHasher(Base64.getEncoder().encodeToString(new byte[31])));
    }

    @Test
    void invalidBase64IsRejected() {
        assertThrows(IllegalStateException.class, () -> new HmacSha256NationalIdHasher("not-base64!"));
    }

    @Test
    void differentKeysProduceDifferentHashes() {
        var other = new HmacSha256NationalIdHasher(Base64.getEncoder().encodeToString(new byte[32]));
        var id = new NationalId("29801011234567");
        assertNotEquals(hasher.hash(id), other.hash(id));
    }

    @Test
    void nullNationalIdIsRejected() {
        assertThrows(NullPointerException.class, () -> hasher.hash(null));
    }
}
