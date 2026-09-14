package com.tuitionnetwork.tuitionnetwork.identity.infra.security;

import com.tuitionnetwork.tuitionnetwork.identity.domain.NationalId;
import com.tuitionnetwork.tuitionnetwork.identity.service.NationalIdEncryptor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.SecureRandom;
import java.util.Base64;

@Component
public class AesGcmNationalIdEncryptor implements NationalIdEncryptor {

    private static final byte FORMAT_VERSION = 1;
    private static final int IV_BYTES = 12;
    private static final int TAG_BITS = 128;

    private final SecretKeySpec key;
    private final SecureRandom secureRandom;

    @Autowired
    public AesGcmNationalIdEncryptor(
            @Value("${security.national-id.encryption-key-base64}") String encodedKey) {
        this(encodedKey, new SecureRandom());
    }

    AesGcmNationalIdEncryptor(String encodedKey, SecureRandom secureRandom) {
        byte[] decoded;
        try {
            decoded = Base64.getDecoder().decode(encodedKey);
        } catch (IllegalArgumentException exception) {
            throw new IllegalStateException("National ID encryption key must be valid Base64", exception);
        }
        if (decoded.length != 32) {
            throw new IllegalStateException("National ID encryption key must decode to exactly 32 bytes");
        }
        this.key = new SecretKeySpec(decoded, "AES");
        this.secureRandom = secureRandom;
    }

    @Override
    public byte[] encrypt(NationalId nationalId) {
        byte[] iv = new byte[IV_BYTES];
        secureRandom.nextBytes(iv);
        try {
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.ENCRYPT_MODE, key, new GCMParameterSpec(TAG_BITS, iv));
            byte[] ciphertext = cipher.doFinal(nationalId.value().getBytes(StandardCharsets.UTF_8));
            return ByteBuffer.allocate(1 + iv.length + ciphertext.length)
                    .put(FORMAT_VERSION)
                    .put(iv)
                    .put(ciphertext)
                    .array();
        } catch (GeneralSecurityException exception) {
            throw new IllegalStateException("Could not encrypt national ID", exception);
        }
    }
}
