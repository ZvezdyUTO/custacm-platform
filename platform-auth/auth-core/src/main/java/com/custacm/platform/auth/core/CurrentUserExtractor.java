package com.custacm.platform.auth.core;

import org.springframework.security.oauth2.jwt.Jwt;

public final class CurrentUserExtractor {
    private CurrentUserExtractor() {
    }

    public static CurrentUser from(Jwt jwt) {
        String studentIdentity = requiredClaimAsString(jwt, "student_identity");
        return new CurrentUser(studentIdentity, KeycloakRoles.platformRoleFromClaims(jwt.getClaims()));
    }

    private static String requiredClaimAsString(Jwt jwt, String claimName) {
        Object value = jwt.getClaims().get(claimName);
        if (value instanceof String text && !text.isBlank()) {
            return text;
        }
        throw new IllegalArgumentException("missing JWT claim: " + claimName);
    }
}
