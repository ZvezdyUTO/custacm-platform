package com.custacm.platform.trainingdata.common.web.collector.request;

import java.time.Duration;

import static com.custacm.platform.trainingdata.common.support.Texts.requireText;

public record OjSubmissionCollectionRequest(
        String username,
        Long lookbackHours,
        String ojName
) {
    public String requireUsername() {
        return requireText(username, "username");
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

    public String optionalOjName() {
        return ojName == null || ojName.isBlank() ? null : ojName.trim();
    }
}
