package com.custacm.platform.trainingdata.common.collector;

import java.time.Instant;
import java.util.List;

public interface OjCollectionHandleResolver {
    String getHandleByStudentIdentity(String ojName, String studentIdentity);

    List<String> listHandlesForCollection(String ojName);

    default void markHandleCollected(
            String ojName,
            String handle,
            boolean historyStartReached,
            Instant collectedAt
    ) {
    }
}
