package com.custacm.platform.auth.core;

import java.util.Locale;
import java.util.Set;

public final class PlatformRoles {
    public static final String ADMIN = "admin";
    public static final String PLAYER = "player";

    private static final Set<String> TOKEN_ROLES = Set.of(ADMIN, PLAYER);

    private PlatformRoles() {
    }

    public static String requireTokenRole(String role) {
        if (role == null || role.isBlank()) {
            throw new IllegalArgumentException("missing platform role: admin or player");
        }
        String normalized = role.trim().toLowerCase(Locale.ROOT);
        if (!TOKEN_ROLES.contains(normalized)) {
            throw new IllegalArgumentException("unsupported platform token role: " + role);
        }
        return normalized;
    }
}
