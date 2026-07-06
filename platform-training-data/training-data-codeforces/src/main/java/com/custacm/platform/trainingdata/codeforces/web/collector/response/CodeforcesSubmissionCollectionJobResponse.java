package com.custacm.platform.trainingdata.codeforces.web.collector.response;

import com.custacm.platform.trainingdata.codeforces.app.collector.job.CodeforcesSubmissionCollectionJobSnapshot;
import com.custacm.platform.trainingdata.codeforces.app.collector.job.CodeforcesSubmissionCollectionJobStatus;

import java.time.Instant;
import java.util.List;

public record CodeforcesSubmissionCollectionJobResponse(
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
        List<CodeforcesSubmissionCollectionJobItemResponse> items
) {
    public static CodeforcesSubmissionCollectionJobResponse from(CodeforcesSubmissionCollectionJobSnapshot snapshot) {
        return new CodeforcesSubmissionCollectionJobResponse(
                snapshot.jobId(),
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
                        .map(CodeforcesSubmissionCollectionJobItemResponse::from)
                        .toList()
        );
    }
}
