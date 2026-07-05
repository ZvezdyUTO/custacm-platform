package com.custacm.platform.trainingdata.codeforces.web.ingest.response;

import java.time.Instant;

public record CodeforcesOdsBatchUpsertResponse(
        String batchId,
        String tableName,
        int writtenRows,
        Instant fetchedAt
) {
}
