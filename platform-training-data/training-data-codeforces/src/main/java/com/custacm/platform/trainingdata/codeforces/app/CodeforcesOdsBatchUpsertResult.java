package com.custacm.platform.trainingdata.codeforces.app;

import java.time.Instant;

public record CodeforcesOdsBatchUpsertResult(
        String batchId,
        String tableName,
        int writtenRows,
        Instant fetchedAt
) {
}
