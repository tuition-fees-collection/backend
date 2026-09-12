package com.tuitionnetwork.tuitionnetwork.identity.api;

public record LoginResponse(String accessToken, String tokenType, long expiresInSeconds) {}
