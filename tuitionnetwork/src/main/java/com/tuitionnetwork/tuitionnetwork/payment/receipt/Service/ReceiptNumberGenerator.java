package com.tuitionnetwork.tuitionnetwork.payment.receipt.service;
import java.security.SecureRandom;
import java.time.Clock;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import org.springframework.stereotype.Component;

@Component
public class ReceiptNumberGenerator {
    private static final String PREFIX = "RCP";
    private static final String ALPHABET = "23456789ABCDEFGHJKLMNPQRSTUVWXYZ";
    private static final int LENGTH = 8;
    private static final ZoneId ZONE = ZoneId.of("Africa/Cairo");
    private final SecureRandom random = new SecureRandom();
    private final Clock clock;
    public ReceiptNumberGenerator() {
        this(Clock.system(ZONE));
    }

    public ReceiptNumberGenerator(Clock clock) {
        this.clock = clock;
    }
 
    public String next() {
        StringBuilder suffix = new StringBuilder(LENGTH);
        for (int i = 0; i < LENGTH; i++) {
            suffix.append(ALPHABET.charAt(random.nextInt(ALPHABET.length())));
        }
        return PREFIX + "-" + ZonedDateTime.now(clock).getYear() + "-" + suffix;
    }
}
