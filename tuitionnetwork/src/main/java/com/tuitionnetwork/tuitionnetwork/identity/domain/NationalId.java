package com.tuitionnetwork.tuitionnetwork.identity.domain;

public record NationalId(String value) {

    public NationalId {
        if (value == null || !value.matches("\\d{14}")) {
            throw new IllegalArgumentException("National ID must contain exactly 14 digits");
        }
    }
}
