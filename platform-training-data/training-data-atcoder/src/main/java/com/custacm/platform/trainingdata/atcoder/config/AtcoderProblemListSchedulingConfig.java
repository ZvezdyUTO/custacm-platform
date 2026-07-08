package com.custacm.platform.trainingdata.atcoder.config;

import com.custacm.platform.trainingdata.atcoder.app.AtcoderProblemListCollectionService;
import com.fasterxml.jackson.core.JsonProcessingException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.SchedulingConfigurer;
import org.springframework.scheduling.config.ScheduledTaskRegistrar;
import org.springframework.scheduling.support.CronTrigger;

import java.time.ZoneId;

@Configuration
public class AtcoderProblemListSchedulingConfig implements SchedulingConfigurer {
    private static final String PROBLEM_LIST_COLLECTION_FAILED_ERROR_CODE =
            "ATCODER_PROBLEM_LIST_COLLECTION_FAILED";
    private static final Logger log = LoggerFactory.getLogger(AtcoderProblemListSchedulingConfig.class);

    private final AtcoderProblemListCollectorProperties properties;
    private final AtcoderProblemListCollectionService collectionService;

    public AtcoderProblemListSchedulingConfig(
            AtcoderProblemListCollectorProperties properties,
            AtcoderProblemListCollectionService collectionService
    ) {
        this.properties = properties;
        this.collectionService = collectionService;
    }

    @Override
    public void configureTasks(ScheduledTaskRegistrar taskRegistrar) {
        if (!properties.enabled()) {
            log.info("AtCoder problem-list collector schedule is disabled");
            return;
        }
        taskRegistrar.addTriggerTask(
                this::collectProblems,
                new CronTrigger(properties.cron(), ZoneId.of(properties.zone()))
        );
        log.info(
                "Registered AtCoder problem-list collector schedule, cron={}, zone={}",
                properties.cron(),
                properties.zone()
        );
    }

    private void collectProblems() {
        try {
            var result = collectionService.collectProblems();
            log.info(
                    "AtCoder problem metadata collection finished, problemBatchId={}, problemModelBatchId={}, writtenRows={}",
                    result.problemResult().batchId(),
                    result.problemModelResult().batchId(),
                    result.writtenRows()
            );
        } catch (JsonProcessingException ex) {
            log.error("AtCoder problem-list collection failed, errorCode={}",
                    PROBLEM_LIST_COLLECTION_FAILED_ERROR_CODE, ex);
        } catch (RuntimeException ex) {
            log.error("AtCoder problem-list collection failed, errorCode={}",
                    PROBLEM_LIST_COLLECTION_FAILED_ERROR_CODE, ex);
        }
    }
}
