package com.custacm.platform.trainingdata.common.collector.result;

import java.time.Instant;

public record OjSubmissionCollectionWriteResult(
        String batchId,
        String tableName,
        int writtenRows,
        Instant fetchedAt
) {
}
