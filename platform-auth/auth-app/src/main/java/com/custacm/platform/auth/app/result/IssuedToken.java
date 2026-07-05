package com.custacm.platform.auth.app.result;

public record IssuedToken(String tokenValue, long expiresInSeconds) {
}
