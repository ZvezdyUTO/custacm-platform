package com.custacm.platform.trainingdata.codeforces.web.collector.request;

import java.time.Duration;
import java.util.List;

public record CodeforcesSubmissionCollectionJobStartRequest(
        List<String> studentIdentities,
        Long lookbackHours,
        Boolean refreshWarehouse
) {
    public List<String> requireStudentIdentities() {
        if (studentIdentities == null || studentIdentities.isEmpty()) {
            throw new IllegalArgumentException("studentIdentities must not be empty");
        }
        return studentIdentities;
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

    public boolean refreshWarehouseOrDefault() {
        return Boolean.TRUE.equals(refreshWarehouse);
    }
}
