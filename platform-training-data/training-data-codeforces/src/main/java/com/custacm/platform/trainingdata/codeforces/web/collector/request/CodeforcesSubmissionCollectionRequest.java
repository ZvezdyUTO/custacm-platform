package com.custacm.platform.trainingdata.codeforces.web.collector.request;

import java.time.Duration;

public record CodeforcesSubmissionCollectionRequest(
        String studentIdentity,
        Long lookbackHours
) {
    public String requireStudentIdentity() {
        if (studentIdentity == null || studentIdentity.isBlank()) {
            throw new IllegalArgumentException("studentIdentity must not be blank");
        }
        return studentIdentity.trim();
    }

    public Duration requireLookbackDuration() {
        if (lookbackHours == null || lookbackHours <= 0) {
            throw new IllegalArgumentException("lookbackHours must be positive");
        }
        try {
            return Duration.ofHours(lookbackHours);
        } catch (ArithmeticException ex) {
            throw new IllegalArgumentException("lookbackHours is too large", ex);
        }
    }
}
