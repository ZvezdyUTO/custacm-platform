package com.custacm.platform.trainingdata.common.collector;

import java.time.Instant;
import java.util.List;

public interface OjCollectionHandleResolver {
    String getHandleByUsername(String ojName, String username);

    List<String> listHandlesForCollection(String ojName);

    default void markHandleCollected(
            String ojName,
            String handle,
            boolean historyStartReached,
            Instant collectedAt
    ) {
    }
}
