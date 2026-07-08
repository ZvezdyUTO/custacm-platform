package com.custacm.platform.trainingdata.common.web.collector.response;

import com.custacm.platform.trainingdata.common.collector.job.OjSubmissionCollectionJobSnapshot;
import com.custacm.platform.trainingdata.common.collector.job.OjSubmissionCollectionJobStatus;

import java.time.Instant;
import java.util.List;

public record OjSubmissionCollectionJobResponse(
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
        List<OjSubmissionCollectionJobItemResponse> items
) {
    public static OjSubmissionCollectionJobResponse from(OjSubmissionCollectionJobSnapshot snapshot) {
        return new OjSubmissionCollectionJobResponse(
                snapshot.jobId(),
                snapshot.ojName(),
                snapshot.status(),
                snapshot.requestedCount(),
                snapshot.completedCount(),
                snapshot.collectedCount(),
                snapshot.failedCount(),
                snapshot.refreshedCount(),
                snapshot.writtenRows(),
                snapshot.batchIds(),
                snapshot.startedAt(),
                snapshot.finishedAt(),
                snapshot.message(),
                snapshot.items().stream()
                        .map(OjSubmissionCollectionJobItemResponse::from)
                        .toList()
        );
    }
}
