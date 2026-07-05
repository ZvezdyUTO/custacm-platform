package com.custacm.platform.auth.web;

import java.time.Instant;

public record UserResponse(
        String studentIdentity,
        String role,
        Instant createdAt,
        Instant updatedAt
) {
}
