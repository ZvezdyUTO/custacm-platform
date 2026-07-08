package com.custacm.platform.trainingdata.common.domain.oj.model;

import java.time.Instant;

public record OjHandleCollectionState(
        boolean historyStartReached,
        Instant lastCollectedAt
) {
    public static OjHandleCollectionState empty() {
        return new OjHandleCollectionState(false, null);
    }

    public OjHandleCollectionState markCollected(boolean reachedHistoryStart, Instant collectedAt) {
        if (collectedAt == null) {
            throw new IllegalArgumentException("collectedAt must not be null");
        }
        return new OjHandleCollectionState(historyStartReached || reachedHistoryStart, collectedAt);
    }
}
