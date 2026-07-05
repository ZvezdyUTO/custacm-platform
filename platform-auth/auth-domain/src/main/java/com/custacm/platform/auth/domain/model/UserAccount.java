package com.custacm.platform.auth.domain.model;

import java.time.Instant;

public record UserAccount(
        String studentIdentity,
        String passwordHash,
        UserRole role,
        Instant createdAt,
        Instant updatedAt
) {
    public UserAccount withPasswordHash(String newPasswordHash, Instant now) {
        return new UserAccount(studentIdentity, newPasswordHash, role, createdAt, now);
    }

    public UserAccount withRole(UserRole newRole, Instant now) {
        return new UserAccount(studentIdentity, passwordHash, newRole, createdAt, now);
    }
}
