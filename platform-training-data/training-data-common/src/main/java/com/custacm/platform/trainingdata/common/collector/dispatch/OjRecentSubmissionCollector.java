package com.custacm.platform.trainingdata.common.collector.dispatch;

import com.custacm.platform.trainingdata.common.collector.result.OjSubmissionCollectionResult;
import com.fasterxml.jackson.core.JsonProcessingException;

import java.time.Duration;

public interface OjRecentSubmissionCollector {
    String ojName();

    OjSubmissionCollectionResult collectRecentWindowForConfiguredHandles(
            Duration lookback
    ) throws JsonProcessingException;

    OjSubmissionCollectionResult collectRecentWindowForUsername(
            String username,
            Duration lookback
    ) throws JsonProcessingException;
}
