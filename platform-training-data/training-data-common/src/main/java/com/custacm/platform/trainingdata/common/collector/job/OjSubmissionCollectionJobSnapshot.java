package com.custacm.platform.trainingdata.common.collector.job;

import java.time.Instant;
import java.util.List;

public record OjSubmissionCollectionJobSnapshot(
        String jobId,
        String ojName,
        OjSubmissionCollectionJobStatus status,
        int requestedCount,
        int completedCount,
        int collectedCount,
        int failedCount,
        int refreshedCount,
        int writtenRows,
        List<String> batchIds,
        Instant startedAt,
        Instant finishedAt,
        String message,
        List<OjSubmissionCollectionJobItem> items
) {
    public OjSubmissionCollectionJobSnapshot {
        batchIds = List.copyOf(batchIds);
        items = List.copyOf(items);
    }
}
