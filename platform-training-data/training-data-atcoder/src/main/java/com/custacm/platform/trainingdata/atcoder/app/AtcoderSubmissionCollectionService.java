package com.custacm.platform.trainingdata.atcoder.app;

import com.custacm.platform.trainingdata.atcoder.config.AtcoderCollectorProperties;
import com.custacm.platform.trainingdata.atcoder.domain.AtcoderSubmissionSourceClient;
import com.custacm.platform.trainingdata.common.app.account.TrainingUserDirectory;
import com.custacm.platform.trainingdata.common.collector.OjCollectionRequestExecutor;
import com.custacm.platform.trainingdata.common.collector.OjHandleAccountCollectionHandleResolver;
import com.custacm.platform.trainingdata.common.collector.OjSubmissionCollectionService;
import com.custacm.platform.trainingdata.common.collector.dispatch.OjRecentSubmissionCollector;
import com.custacm.platform.trainingdata.common.collector.result.OjSubmissionCollectionResult;
import com.custacm.platform.trainingdata.common.domain.oj.value.OjNames;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.time.Clock;
import java.time.Duration;

public class AtcoderSubmissionCollectionService implements OjRecentSubmissionCollector {
    private final OjSubmissionCollectionService delegate;

    public AtcoderSubmissionCollectionService(
            TrainingUserDirectory handleAccountService,
            AtcoderSubmissionSourceClient sourceClient,
            AtcoderOdsIngestService ingestService,
            ObjectMapper objectMapper,
            AtcoderCollectorProperties properties
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

    public AtcoderSubmissionCollectionService(
            TrainingUserDirectory handleAccountService,
            AtcoderSubmissionSourceClient sourceClient,
            AtcoderOdsIngestService ingestService,
            ObjectMapper objectMapper,
            int pageSize,
            int maxRequestAttempts,
            Duration requestInterval,
            Clock clock,
            OjCollectionRequestExecutor.SleepStrategy sleepStrategy
    ) {
        this.delegate = new OjSubmissionCollectionService(
                new OjHandleAccountCollectionHandleResolver(handleAccountService),
                new AtcoderSubmissionCollectionAdapter(sourceClient, ingestService, objectMapper, pageSize),
                maxRequestAttempts,
                requestInterval,
                clock,
                sleepStrategy
        );
    }

    @Override
    public String ojName() {
        return OjNames.ATCODER;
    }

    @Override
    public OjSubmissionCollectionResult collectRecentWindowForConfiguredHandles(
            Duration lookback
    ) throws JsonProcessingException {
        return delegate.collectRecentWindowForConfiguredHandles(OjNames.ATCODER, lookback);
    }

    @Override
    public OjSubmissionCollectionResult collectRecentWindowForUsername(
            String username,
            Duration lookback
    ) throws JsonProcessingException {
        return delegate.collectRecentWindowForUsername(OjNames.ATCODER, username, lookback);
    }

}
