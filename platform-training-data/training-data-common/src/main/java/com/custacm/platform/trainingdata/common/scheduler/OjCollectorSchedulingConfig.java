package com.custacm.platform.trainingdata.common.scheduler;

import com.custacm.platform.trainingdata.common.collector.OjCollectionExecutionCoordinator;
import com.custacm.platform.trainingdata.common.collector.config.OjCollectorSchedulingProperties;
import com.custacm.platform.trainingdata.common.collector.job.OjSubmissionCollectionJobRefreshResult;
import com.custacm.platform.trainingdata.common.collector.job.OjSubmissionCollectionJobRefreshStatus;
import com.custacm.platform.trainingdata.common.collector.job.OjWarehouseRefreshDispatcher;
import com.custacm.platform.trainingdata.common.collector.result.OjSubmissionCollectionResult;
import com.fasterxml.jackson.core.JsonProcessingException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.SchedulingConfigurer;
import org.springframework.scheduling.config.ScheduledTaskRegistrar;
import org.springframework.scheduling.support.CronTrigger;

import java.time.ZoneId;
import java.util.List;
import java.util.concurrent.Executor;

@Configuration
@EnableScheduling
public class OjCollectorSchedulingConfig implements SchedulingConfigurer {
    private static final String SCHEDULED_COLLECTION_FAILED_ERROR_CODE =
            "OJ_SCHEDULED_COLLECTION_FAILED";
    private static final String SCHEDULED_WAREHOUSE_REFRESH_FAILED_ERROR_CODE =
            "OJ_SCHEDULED_WAREHOUSE_REFRESH_FAILED";
    private static final String SCHEDULED_COLLECTION_ALREADY_RUNNING_ERROR_CODE =
            "OJ_SCHEDULED_COLLECTION_ALREADY_RUNNING";
    private static final String SCHEDULED_COLLECTION_SUBMIT_FAILED_ERROR_CODE =
            "OJ_SCHEDULED_COLLECTION_SUBMIT_FAILED";
    private static final Logger log = LoggerFactory.getLogger(OjCollectorSchedulingConfig.class);

    private final OjCollectorSchedulingProperties properties;
    private final OjScheduledSubmissionCollectionService collectionService;
    private final OjWarehouseRefreshDispatcher warehouseRefreshDispatcher;
    private final Executor scheduledCollectionExecutor;
    private final OjCollectionExecutionCoordinator executionCoordinator;

    public OjCollectorSchedulingConfig(
            OjCollectorSchedulingProperties properties,
            OjScheduledSubmissionCollectionService collectionService,
            OjWarehouseRefreshDispatcher warehouseRefreshDispatcher,
            @Qualifier("ojScheduledCollectionExecutor") Executor scheduledCollectionExecutor,
            OjCollectionExecutionCoordinator executionCoordinator
    ) {
        this.properties = properties;
        this.collectionService = collectionService;
        this.warehouseRefreshDispatcher = warehouseRefreshDispatcher;
        this.scheduledCollectionExecutor = scheduledCollectionExecutor;
        this.executionCoordinator = executionCoordinator;
    }

    @Override
    public void configureTasks(ScheduledTaskRegistrar taskRegistrar) {
        List<OjCollectorSchedulingProperties.Schedule> schedules = properties.enabledSchedules();
        if (schedules.isEmpty()) {
            log.info("No enabled OJ collector schedules configured");
            return;
        }
        for (OjCollectorSchedulingProperties.Schedule schedule : schedules) {
            taskRegistrar.addTriggerTask(
                    () -> submitScheduledWindow(schedule),
                    new CronTrigger(schedule.cron(), ZoneId.of(schedule.zone()))
            );
            log.info(
                    "Registered OJ collector schedule, ojName={}, name={}, cron={}, zone={}, lookback={}",
                    schedule.ojName(),
                    schedule.name(),
                    schedule.cron(),
                    schedule.zone(),
                    schedule.lookback()
            );
        }
    }

    private void submitScheduledWindow(OjCollectorSchedulingProperties.Schedule schedule) {
        var acquired = executionCoordinator.tryAcquire(schedule.ojName());
        if (acquired.isEmpty()) {
            log.warn(
                    "Scheduled OJ submission collection skipped, errorCode={}, ojName={}, schedule={}",
                    SCHEDULED_COLLECTION_ALREADY_RUNNING_ERROR_CODE,
                    schedule.ojName(),
                    schedule.name()
            );
            return;
        }
        OjCollectionExecutionCoordinator.Permit permit = acquired.orElseThrow();
        try {
            scheduledCollectionExecutor.execute(() -> {
                try (permit) {
                    collectScheduledWindow(schedule);
                }
            });
        } catch (RuntimeException ex) {
            permit.close();
            log.error(
                    "Failed to submit scheduled OJ submission collection, errorCode={}, ojName={}, schedule={}",
                    SCHEDULED_COLLECTION_SUBMIT_FAILED_ERROR_CODE,
                    schedule.ojName(),
                    schedule.name(),
                    ex
            );
        }
    }

    private void collectScheduledWindow(OjCollectorSchedulingProperties.Schedule schedule) {
        try {
            var result = collectionService.collectRecentWindowForConfiguredHandles(
                    schedule.ojName(),
                    schedule.lookback()
            );
            OjSubmissionCollectionJobRefreshResult refreshResult = refreshWarehouse(result);
            log.info(
                    "Scheduled OJ submission collection finished, ojName={}, schedule={}, status={}, "
                            + "requestedHandleCount={}, failedHandleCount={}, writtenRows={}, "
                            + "warehouseRefreshStatus={}",
                    schedule.ojName(),
                    schedule.name(),
                    result.status(),
                    result.requestedHandleCount(),
                    result.failedHandleCount(),
                    result.writtenRows(),
                    refreshResult.status()
            );
        } catch (JsonProcessingException ex) {
            log.error("Scheduled OJ submission collection failed, errorCode={}, ojName={}, schedule={}",
                    SCHEDULED_COLLECTION_FAILED_ERROR_CODE, schedule.ojName(), schedule.name(), ex);
        } catch (RuntimeException ex) {
            log.error("Scheduled OJ submission collection failed, errorCode={}, ojName={}, schedule={}",
                    SCHEDULED_COLLECTION_FAILED_ERROR_CODE, schedule.ojName(), schedule.name(), ex);
        }
    }

    private OjSubmissionCollectionJobRefreshResult refreshWarehouse(OjSubmissionCollectionResult result) {
        if (result.batchId() == null || result.batchId().isBlank()) {
            return OjSubmissionCollectionJobRefreshResult.noBatch();
        }
        try {
            OjSubmissionCollectionJobRefreshResult refreshResult = warehouseRefreshDispatcher.refresh(result);
            if (refreshResult.status() == OjSubmissionCollectionJobRefreshStatus.FAILED) {
                log.error(
                        "Scheduled OJ warehouse refresh failed, errorCode={}, ojName={}, batchId={}, message={}",
                        SCHEDULED_WAREHOUSE_REFRESH_FAILED_ERROR_CODE,
                        result.ojName(),
                        result.batchId(),
                        refreshResult.message()
                );
            }
            return refreshResult;
        } catch (RuntimeException ex) {
            log.error(
                    "Scheduled OJ warehouse refresh failed, errorCode={}, ojName={}, batchId={}",
                    SCHEDULED_WAREHOUSE_REFRESH_FAILED_ERROR_CODE,
                    result.ojName(),
                    result.batchId(),
                    ex
            );
            return OjSubmissionCollectionJobRefreshResult.failed(ex.getMessage());
        }
    }
}
