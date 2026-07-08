package com.custacm.platform.trainingdata.atcoder.app;

import java.time.Instant;

public record AtcoderOdsBatchUpsertResult(
        String batchId,
        String tableName,
        int writtenRows,
        Instant fetchedAt
) {
}
