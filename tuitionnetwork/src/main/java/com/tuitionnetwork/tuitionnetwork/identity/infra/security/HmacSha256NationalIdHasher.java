package com.tuitionnetwork.tuitionnetwork.identity.infra.security;

import com.tuitionnetwork.tuitionnetwork.identity.domain.NationalId;
import com.tuitionnetwork.tuitionnetwork.identity.service.NationalIdHasher;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.HexFormat;
import java.util.Objects;

@Component
public class HmacSha256NationalIdHasher implements NationalIdHasher {

    private static final String ALGORITHM = "HmacSHA256";

    private final SecretKeySpec key;

    public HmacSha256NationalIdHasher(
            @Value("${security.national-id.hmac-key-base64}") String keyBase64) {
        byte[] keyBytes;
        try {
            keyBytes = Base64.getDecoder().decode(keyBase64);
        } catch (IllegalArgumentException e) {
            throw new IllegalStateException("National ID HMAC key must be valid Base64", e);
        }
        if (keyBytes.length < 32) {
            throw new IllegalStateException("National ID HMAC key must be at least 256 bits");
        }
        this.key = new SecretKeySpec(keyBytes, ALGORITHM);
    }

    @Override
    public String hash(NationalId nationalId) {
        Objects.requireNonNull(nationalId, "nationalId");
        try {
            Mac mac = Mac.getInstance(ALGORITHM);
            mac.init(key);
            byte[] digest = mac.doFinal(nationalId.value().getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (Exception e) {
            throw new IllegalStateException("Unable to compute National ID HMAC", e);
        }
    }
}
