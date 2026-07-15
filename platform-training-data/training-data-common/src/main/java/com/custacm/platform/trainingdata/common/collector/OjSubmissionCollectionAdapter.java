package com.custacm.platform.trainingdata.common.collector;

import com.custacm.platform.trainingdata.common.collector.result.OjHandleCollectionOutcome;
import com.fasterxml.jackson.core.JsonProcessingException;

import java.time.Instant;

public interface OjSubmissionCollectionAdapter {
    String defaultOjName();

    default String displayName(String ojName) {
        return ojName;
    }

    OjSubmissionCollectionBatchWriter openBatch(String ojName);

    OjHandleCollectionOutcome collectHandle(
            String ojName,
            String handle,
            Instant windowStartInclusive,
            Instant windowEndExclusive,
            OjCollectionRequestExecutor requestExecutor,
            OjSubmissionCollectionBatchWriter batchWriter
    ) throws JsonProcessingException;
}
