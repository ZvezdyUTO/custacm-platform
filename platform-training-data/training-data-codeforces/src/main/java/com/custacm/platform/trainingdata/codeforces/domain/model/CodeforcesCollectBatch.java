package com.custacm.platform.trainingdata.codeforces.domain.model;

import java.time.Instant;
import java.util.Objects;

public record CodeforcesCollectBatch(
        String batchId,
        Instant fetchedAt
) {
    public CodeforcesCollectBatch {
        if (batchId == null || batchId.isBlank()) {
            throw new IllegalArgumentException("batchId must not be blank");
        }
        Objects.requireNonNull(fetchedAt, "fetchedAt must not be null");
    }
}
