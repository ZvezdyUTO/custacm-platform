package com.custacm.platform.trainingdata.atcoder.domain;

import java.time.Instant;
import java.util.Objects;

import static com.custacm.platform.trainingdata.common.support.Texts.requireText;

public record AtcoderCollectBatch(
        String batchId,
        Instant fetchedAt
) {
    public AtcoderCollectBatch {
        requireText(batchId, "batchId");
        Objects.requireNonNull(fetchedAt, "fetchedAt must not be null");
    }
}
