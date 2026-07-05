package com.custacm.platform.trainingdata.codeforces.app.result;

import java.time.Instant;

public record CodeforcesOdsBatchUpsertResult(
        String batchId,
        String tableName,
        int writtenRows,
        Instant fetchedAt
) {
}
