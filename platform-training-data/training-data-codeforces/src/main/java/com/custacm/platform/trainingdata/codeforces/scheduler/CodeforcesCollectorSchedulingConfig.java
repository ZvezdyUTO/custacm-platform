package com.custacm.platform.trainingdata.codeforces.scheduler;

import com.custacm.platform.trainingdata.codeforces.app.collector.CodeforcesSubmissionCollectionService;
import com.custacm.platform.trainingdata.codeforces.collector.config.CodeforcesCollectorProperties;
import com.fasterxml.jackson.core.JsonProcessingException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.SchedulingConfigurer;
import org.springframework.scheduling.config.ScheduledTaskRegistrar;
import org.springframework.scheduling.support.CronTrigger;

import java.time.ZoneId;
import java.util.List;

@Configuration
@EnableScheduling
public class CodeforcesCollectorSchedulingConfig implements SchedulingConfigurer {
    private static final String SCHEDULED_COLLECTION_FAILED_ERROR_CODE =
            "CODEFORCES_SCHEDULED_COLLECTION_FAILED";
    private static final Logger log = LoggerFactory.getLogger(CodeforcesCollectorSchedulingConfig.class);

    private final CodeforcesCollectorProperties properties;
    private final CodeforcesSubmissionCollectionService collectionService;

    public CodeforcesCollectorSchedulingConfig(
            CodeforcesCollectorProperties properties,
            CodeforcesSubmissionCollectionService collectionService
    ) {
        this.properties = properties;
        this.collectionService = collectionService;
    }

    @Override
    public void configureTasks(ScheduledTaskRegistrar taskRegistrar) {
        List<CodeforcesCollectorProperties.Schedule> schedules = properties.enabledSchedules();
        if (schedules.isEmpty()) {
            log.info("No enabled Codeforces collector schedules configured");
            return;
        }
        for (CodeforcesCollectorProperties.Schedule schedule : schedules) {
            taskRegistrar.addTriggerTask(
                    () -> collectScheduledWindow(schedule),
                    new CronTrigger(schedule.cron(), ZoneId.of(schedule.zone()))
            );
            log.info(
                    "Registered Codeforces collector schedule, name={}, cron={}, zone={}, lookback={}",
                    schedule.name(),
                    schedule.cron(),
                    schedule.zone(),
                    schedule.lookback()
            );
        }
    }

    private void collectScheduledWindow(CodeforcesCollectorProperties.Schedule schedule) {
        try {
            var result = collectionService.collectRecentWindowForConfiguredHandles(schedule.lookback());
            log.info(
                    "Scheduled Codeforces submission collection finished, schedule={}, status={}, requestedHandleCount={}, "
                            + "failedHandleCount={}, writtenRows={}",
                    schedule.name(),
                    result.status(),
                    result.requestedHandleCount(),
                    result.failedHandleCount(),
                    result.writtenRows()
            );
        } catch (JsonProcessingException ex) {
            log.error("Scheduled Codeforces submission collection failed, errorCode={}, schedule={}",
                    SCHEDULED_COLLECTION_FAILED_ERROR_CODE, schedule.name(), ex);
        } catch (RuntimeException ex) {
            log.error("Scheduled Codeforces submission collection failed, errorCode={}, schedule={}",
                    SCHEDULED_COLLECTION_FAILED_ERROR_CODE, schedule.name(), ex);
        }
    }
}
