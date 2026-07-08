package com.custacm.platform.trainingdata.atcoder.app;

import com.custacm.platform.trainingdata.atcoder.config.AtcoderCollectorProperties;
import com.custacm.platform.trainingdata.atcoder.domain.AtcoderProblemSourceClient;
import com.custacm.platform.trainingdata.common.collector.OjCollectionRequestExecutor;
import com.fasterxml.jackson.core.JsonProcessingException;

import java.time.Duration;

public class AtcoderProblemListCollectionService {
    private static final String PROBLEM_BATCH_ID_PREFIX = "collector-atcoder-problems";

    private final AtcoderProblemSourceClient sourceClient;
    private final AtcoderOdsIngestService ingestService;
    private final int maxRequestAttempts;
    private final Duration requestInterval;
    private final OjCollectionRequestExecutor.SleepStrategy sleepStrategy;

    public AtcoderProblemListCollectionService(
            AtcoderProblemSourceClient sourceClient,
            AtcoderOdsIngestService ingestService,
            AtcoderCollectorProperties properties
    ) {
        this(
                sourceClient,
                ingestService,
                properties.maxRequestAttempts(),
                properties.requestInterval(),
                duration -> Thread.sleep(duration.toMillis())
        );
    }

    public AtcoderProblemListCollectionService(
            AtcoderProblemSourceClient sourceClient,
            AtcoderOdsIngestService ingestService,
            int maxRequestAttempts,
            Duration requestInterval,
            OjCollectionRequestExecutor.SleepStrategy sleepStrategy
    ) {
        if (maxRequestAttempts <= 0) {
            throw new IllegalArgumentException("maxRequestAttempts must be positive");
        }
        this.sourceClient = sourceClient;
        this.ingestService = ingestService;
        this.maxRequestAttempts = maxRequestAttempts;
        this.requestInterval = requestInterval == null || requestInterval.isNegative()
                ? Duration.ZERO
                : requestInterval;
        this.sleepStrategy = sleepStrategy;
    }

    public AtcoderOdsBatchUpsertResult collectProblems() throws JsonProcessingException {
        OjCollectionRequestExecutor requestExecutor = new OjCollectionRequestExecutor(
                maxRequestAttempts,
                requestInterval,
                sleepStrategy
        );
        return ingestService.upsertProblems(
                requestExecutor.execute(sourceClient::fetchProblems),
                PROBLEM_BATCH_ID_PREFIX
        );
    }
}
