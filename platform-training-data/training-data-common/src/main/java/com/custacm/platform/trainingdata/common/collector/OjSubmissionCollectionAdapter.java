package com.custacm.platform.trainingdata.common.collector;

import com.custacm.platform.trainingdata.common.collector.result.OjHandleCollectionOutcome;
import com.custacm.platform.trainingdata.common.collector.result.OjSubmissionCollectionWriteResult;
import com.fasterxml.jackson.core.JsonProcessingException;

import java.time.Instant;
import java.util.List;

public interface OjSubmissionCollectionAdapter {
    String defaultOjName();

    default String displayName(String ojName) {
        return ojName;
    }

    OjHandleCollectionOutcome collectHandle(
            String ojName,
            String handle,
            Instant windowStartInclusive,
            Instant windowEndExclusive,
            OjCollectionRequestExecutor requestExecutor
    );

    OjSubmissionCollectionWriteResult writeBatch(
            String ojName,
            List<OjHandleCollectionOutcome> outcomes
    ) throws JsonProcessingException;
}
