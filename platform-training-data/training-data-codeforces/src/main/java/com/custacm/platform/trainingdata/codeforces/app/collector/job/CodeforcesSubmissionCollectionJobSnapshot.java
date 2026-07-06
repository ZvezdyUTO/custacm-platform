package com.custacm.platform.trainingdata.codeforces.app.collector.job;

import java.time.Instant;
import java.util.List;

public record CodeforcesSubmissionCollectionJobSnapshot(
        String jobId,
        CodeforcesSubmissionCollectionJobStatus status,
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
        List<CodeforcesSubmissionCollectionJobItem> items
) {
    public CodeforcesSubmissionCollectionJobSnapshot {
        batchIds = List.copyOf(batchIds);
        items = List.copyOf(items);
    }
}
