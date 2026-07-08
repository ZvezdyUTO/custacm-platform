package com.custacm.platform.trainingdata.codeforces.app;

import com.custacm.platform.trainingdata.common.app.account.OjHandleAccountService;
import com.custacm.platform.trainingdata.common.collector.OjCollectionRequestExecutor;
import com.custacm.platform.trainingdata.common.collector.OjHandleAccountCollectionHandleResolver;
import com.custacm.platform.trainingdata.common.collector.OjSubmissionCollectionService;
import com.custacm.platform.trainingdata.common.collector.dispatch.OjRecentSubmissionCollector;
import com.custacm.platform.trainingdata.common.collector.result.OjSubmissionCollectionResult;
import com.custacm.platform.trainingdata.common.domain.oj.value.OjNames;
import com.custacm.platform.trainingdata.codeforces.config.CodeforcesCollectorProperties;
import com.custacm.platform.trainingdata.codeforces.domain.CodeforcesSubmissionSourceClient;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.time.Clock;
import java.time.Duration;

public class CodeforcesSubmissionCollectionService implements OjRecentSubmissionCollector {
    private final OjSubmissionCollectionService delegate;

    public CodeforcesSubmissionCollectionService(
            OjHandleAccountService handleAccountService,
            CodeforcesSubmissionSourceClient sourceClient,
            CodeforcesOdsSubmissionIngestService ingestService,
            ObjectMapper objectMapper,
            CodeforcesCollectorProperties properties
    ) {
        this(
                handleAccountService,
                sourceClient,
                ingestService,
                objectMapper,
                properties.pageSize(),
                properties.maxRequestAttempts(),
                properties.requestInterval(),
                Clock.systemUTC(),
                duration -> Thread.sleep(duration.toMillis())
        );
    }

    public CodeforcesSubmissionCollectionService(
            OjHandleAccountService handleAccountService,
            CodeforcesSubmissionSourceClient sourceClient,
            CodeforcesOdsSubmissionIngestService ingestService,
            ObjectMapper objectMapper,
            int pageSize,
            int maxRequestAttempts,
            Duration requestInterval,
            Clock clock,
            OjCollectionRequestExecutor.SleepStrategy sleepStrategy
    ) {
        this.delegate = new OjSubmissionCollectionService(
                new OjHandleAccountCollectionHandleResolver(handleAccountService),
                new CodeforcesSubmissionCollectionAdapter(sourceClient, ingestService, objectMapper, pageSize),
                maxRequestAttempts,
                requestInterval,
                clock,
                sleepStrategy
        );
    }

    @Override
    public String ojName() {
        return OjNames.CODEFORCES;
    }

    @Override
    public OjSubmissionCollectionResult collectRecentWindowForConfiguredHandles(
            Duration lookback
    ) throws JsonProcessingException {
        return delegate.collectRecentWindowForConfiguredHandles(OjNames.CODEFORCES, lookback);
    }

    @Override
    public OjSubmissionCollectionResult collectRecentWindowForStudentIdentity(
            String studentIdentity,
            Duration lookback
    ) throws JsonProcessingException {
        return delegate.collectRecentWindowForStudentIdentity(
                OjNames.CODEFORCES,
                studentIdentity,
                lookback
        );
    }

}
