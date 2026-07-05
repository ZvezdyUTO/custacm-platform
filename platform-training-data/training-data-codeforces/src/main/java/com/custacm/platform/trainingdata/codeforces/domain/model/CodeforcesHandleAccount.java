package com.custacm.platform.trainingdata.codeforces.domain.model;

import java.time.Instant;
import java.util.Objects;

public record CodeforcesHandleAccount(
        String studentIdentity,
        String handle,
        Instant createdAt,
        Instant updatedAt
) {
    public CodeforcesHandleAccount {
        requireText(studentIdentity, "studentIdentity");
        requireText(handle, "handle");
        Objects.requireNonNull(createdAt, "createdAt must not be null");
        Objects.requireNonNull(updatedAt, "updatedAt must not be null");
    }

    private static void requireText(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(fieldName + " must not be blank");
        }
    }
}
