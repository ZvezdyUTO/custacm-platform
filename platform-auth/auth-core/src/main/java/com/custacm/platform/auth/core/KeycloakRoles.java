package com.custacm.platform.auth.core;

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

public final class KeycloakRoles {
    public static final String ADMIN = "admin";
    public static final String STUDENT = "student";

    private KeycloakRoles() {
    }

    public static String platformRoleFromClaims(Map<String, Object> claims) {
        Set<String> roles = fromClaims(claims);
        return platformRoleFrom(roles)
                .orElseThrow(() -> new IllegalArgumentException("missing platform role: admin or student"));
    }

    public static Optional<String> platformRoleFrom(Set<String> roles) {
        if (roles.contains(ADMIN)) {
            return Optional.of(ADMIN);
        }
        if (roles.contains(STUDENT)) {
            return Optional.of(STUDENT);
        }
        return Optional.empty();
    }

    public static Set<String> fromClaims(Map<String, Object> claims) {
        Set<String> roles = new LinkedHashSet<>();
        addRealmRoles(claims.get("realm_access"), roles);
        addResourceRoles(claims.get("resource_access"), roles);
        return Collections.unmodifiableSet(roles);
    }

    private static void addRealmRoles(Object realmAccessClaim, Set<String> roles) {
        if (realmAccessClaim instanceof Map<?, ?> realmAccess) {
            addRoles(realmAccess.get("roles"), roles);
        }
    }

    private static void addResourceRoles(Object resourceAccessClaim, Set<String> roles) {
        if (!(resourceAccessClaim instanceof Map<?, ?> resourceAccess)) {
            return;
        }
        for (Object clientAccessClaim : resourceAccess.values()) {
            if (clientAccessClaim instanceof Map<?, ?> clientAccess) {
                addRoles(clientAccess.get("roles"), roles);
            }
        }
    }

    private static void addRoles(Object rolesClaim, Set<String> roles) {
        if (!(rolesClaim instanceof Collection<?> roleValues)) {
            return;
        }
        for (Object roleValue : roleValues) {
            if (roleValue instanceof String role && !role.isBlank()) {
                roles.add(role.trim());
            }
        }
    }
}
