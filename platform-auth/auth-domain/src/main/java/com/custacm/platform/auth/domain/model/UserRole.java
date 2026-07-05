package com.custacm.platform.auth.domain.model;

import java.util.Locale;

public enum UserRole {
    DISABLE(0),
    PLAYER(1),
    ADMIN(2);

    private final int level;

    UserRole(int level) {
        this.level = level;
    }

    public String value() {
        return name().toLowerCase(Locale.ROOT);
    }

    public boolean canAuthenticate() {
        return this == PLAYER || this == ADMIN;
    }

    public boolean includes(UserRole other) {
        if (this == DISABLE || other == DISABLE) {
            return this == other;
        }
        return level >= other.level;
    }

    public static UserRole fromValue(String value) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("role must not be blank");
        }
        try {
            return UserRole.valueOf(value.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException ex) {
            throw new IllegalArgumentException("unsupported role: " + value, ex);
        }
    }
}
