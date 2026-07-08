package com.custacm.platform.trainingdata.common.scheduler;

import com.custacm.platform.trainingdata.common.collector.result.OjSubmissionCollectionResult;
import com.fasterxml.jackson.core.JsonProcessingException;

import java.time.Duration;

public interface OjScheduledSubmissionCollectionService {
    OjSubmissionCollectionResult collectRecentWindowForConfiguredHandles(
            String ojName,
            Duration lookback
    ) throws JsonProcessingException;

    OjSubmissionCollectionResult collectRecentWindowForStudentIdentity(
            String ojName,
            String studentIdentity,
            Duration lookback
    ) throws JsonProcessingException;
}
