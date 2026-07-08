package com.custacm.platform.auth.web;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

@ConfigurationProperties(prefix = "platform.auth")
public record AuthProperties(Jwt jwt, BootstrapAdmin bootstrapAdmin) {
    public record Jwt(
            String privateKey,
            String privateKeyPath,
            String publicKey,
            String publicKeyPath,
            Duration accessTokenTtl,
            Duration rememberMeAccessTokenTtl
    ) {
        public Duration resolvedAccessTokenTtl() {
            return accessTokenTtl == null ? Duration.ofHours(2) : accessTokenTtl;
        }

        public Duration resolvedRememberMeAccessTokenTtl() {
            return rememberMeAccessTokenTtl == null ? Duration.ofDays(30) : rememberMeAccessTokenTtl;
        }
    }

    public record BootstrapAdmin(String studentIdentity, String password) {
        public boolean configured() {
            return hasText(studentIdentity) || hasText(password);
        }

        public boolean complete() {
            return hasText(studentIdentity) && hasText(password);
        }
    }

    private static boolean hasText(String value) {
        return value != null && !value.isBlank();
    }
}
