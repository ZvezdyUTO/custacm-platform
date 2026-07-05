package com.custacm.platform.auth.core;

import org.springframework.security.oauth2.jwt.Jwt;

public final class CurrentUserExtractor {
    private CurrentUserExtractor() {
    }

    public static CurrentUser from(Jwt jwt) {
        String studentIdentity = requiredSubject(jwt);
        String role = requiredClaimAsString(jwt, "role");
        return new CurrentUser(studentIdentity, PlatformRoles.requireTokenRole(role));
    }

    private static String requiredSubject(Jwt jwt) {
        String subject = jwt.getSubject();
        if (subject != null && !subject.isBlank()) {
            return subject.trim();
        }
        throw new IllegalArgumentException("missing JWT subject: sub");
    }

    private static String requiredClaimAsString(Jwt jwt, String claimName) {
        Object value = jwt.getClaims().get(claimName);
        if (value instanceof String text && !text.isBlank()) {
            return text.trim();
        }
        throw new IllegalArgumentException("missing JWT claim: " + claimName);
    }
}
