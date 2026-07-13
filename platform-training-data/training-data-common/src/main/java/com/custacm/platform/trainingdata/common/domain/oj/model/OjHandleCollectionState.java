package com.custacm.platform.trainingdata.common.domain.oj.model;

import java.time.Instant;

public record OjHandleCollectionState(
        Instant lastCollectedAt
) {
    public static OjHandleCollectionState empty() {
        return new OjHandleCollectionState(null);
    }

    public OjHandleCollectionState markCollected(Instant collectedAt) {
        if (collectedAt == null) {
            throw new IllegalArgumentException("collectedAt must not be null");
        }
        return new OjHandleCollectionState(collectedAt);
    }
}
