package com.custacm.platform.trainingdata.common.collector.dispatch;

import com.custacm.platform.trainingdata.common.collector.result.OjSubmissionCollectionResult;
import com.custacm.platform.trainingdata.common.domain.oj.value.OjNames;
import com.custacm.platform.trainingdata.common.scheduler.OjScheduledSubmissionCollectionService;
import com.fasterxml.jackson.core.JsonProcessingException;

import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public class OjSubmissionCollectionDispatcher implements OjScheduledSubmissionCollectionService {
    private final String defaultOjName;
    private final Map<String, OjRecentSubmissionCollector> collectors;

    public OjSubmissionCollectionDispatcher(
            String defaultOjName,
            List<OjRecentSubmissionCollector> collectors
    ) {
        this.defaultOjName = OjNames.normalize(defaultOjName);
        this.collectors = collectorMap(collectors);
    }

    @Override
    public OjSubmissionCollectionResult collectRecentWindowForConfiguredHandles(
            String ojName,
            Duration lookback
    ) throws JsonProcessingException {
        return collector(ojName).collectRecentWindowForConfiguredHandles(lookback);
    }

    @Override
    public OjSubmissionCollectionResult collectRecentWindowForStudentIdentity(
            String ojName,
            String studentIdentity,
            Duration lookback
    ) throws JsonProcessingException {
        return collector(ojName).collectRecentWindowForStudentIdentity(studentIdentity, lookback);
    }

    private OjRecentSubmissionCollector collector(String ojName) {
        String normalizedOjName = ojName == null || ojName.isBlank()
                ? defaultOjName
                : OjNames.normalize(ojName);
        OjRecentSubmissionCollector collector = collectors.get(normalizedOjName);
        if (collector == null) {
            throw new IllegalArgumentException(normalizedOjName + " submission collection is not implemented");
        }
        return collector;
    }

    private static Map<String, OjRecentSubmissionCollector> collectorMap(
            List<OjRecentSubmissionCollector> collectors
    ) {
        if (collectors == null || collectors.isEmpty()) {
            return Map.of();
        }
        Map<String, OjRecentSubmissionCollector> collectorMap = new LinkedHashMap<>();
        for (OjRecentSubmissionCollector collector : collectors) {
            OjRecentSubmissionCollector nonNullCollector =
                    Objects.requireNonNull(collector, "collector must not be null");
            String normalizedOjName = OjNames.normalize(nonNullCollector.ojName());
            if (collectorMap.putIfAbsent(normalizedOjName, nonNullCollector) != null) {
                throw new IllegalArgumentException("duplicate OJ submission collector: " + normalizedOjName);
            }
        }
        return Map.copyOf(collectorMap);
    }
}
