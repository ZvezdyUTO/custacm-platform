package com.custacm.platform.auth.web;

public record LoginResponse(
        String tokenType,
        String accessToken,
        long expiresInSeconds,
        CurrentUserResponse user
) {
}
