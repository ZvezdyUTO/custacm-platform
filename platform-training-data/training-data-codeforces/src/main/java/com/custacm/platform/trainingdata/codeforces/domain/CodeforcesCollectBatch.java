package com.custacm.platform.trainingdata.codeforces.domain;

import java.time.Instant;
import java.util.Objects;

import static com.custacm.platform.trainingdata.common.support.Texts.requireText;

public record CodeforcesCollectBatch(
        String batchId,
        Instant fetchedAt
) {
    public CodeforcesCollectBatch {
        requireText(batchId, "batchId");
        Objects.requireNonNull(fetchedAt, "fetchedAt must not be null");
    }
}
