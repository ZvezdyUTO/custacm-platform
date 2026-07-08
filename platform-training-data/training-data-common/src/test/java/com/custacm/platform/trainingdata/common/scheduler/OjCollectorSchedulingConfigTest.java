package com.custacm.platform.trainingdata.common.scheduler;

import com.custacm.platform.trainingdata.common.collector.config.OjCollectorSchedulingProperties;
import com.custacm.platform.trainingdata.common.collector.job.OjSubmissionCollectionJobRefreshResult;
import com.custacm.platform.trainingdata.common.collector.job.OjSubmissionCollectionJobRefreshStatus;
import com.custacm.platform.trainingdata.common.collector.job.OjWarehouseRefreshDispatcher;
import com.custacm.platform.trainingdata.common.collector.job.OjWarehouseRefreshHandler;
import com.custacm.platform.trainingdata.common.collector.result.OjSubmissionCollectionResult;
import com.custacm.platform.trainingdata.common.collector.result.OjSubmissionCollectionStatus;
import org.junit.jupiter.api.Test;
import org.springframework.scheduling.config.ScheduledTaskRegistrar;

import java.time.Duration;
import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class OjCollectorSchedulingConfigTest {
    @Test
    void registersOnlyEnabledCollectorSchedulesFromPropertiesAndPassesOjName() throws Exception {
        OjScheduledSubmissionCollectionService collectionService = mock(OjScheduledSubmissionCollectionService.class);
        when(collectionService.collectRecentWindowForConfiguredHandles("CODEFORCES", Duration.ofHours(120)))
                .thenReturn(successResult("CODEFORCES", "batch-codeforces"));
        when(collectionService.collectRecentWindowForConfiguredHandles("ATCODER", Duration.ofHours(48)))
                .thenReturn(successResult("ATCODER", "batch-atcoder"));
        OjWarehouseRefreshHandler codeforcesRefreshHandler = refreshHandler("CODEFORCES", "batch-codeforces");
        OjWarehouseRefreshHandler atcoderRefreshHandler = refreshHandler("ATCODER", "batch-atcoder");
        OjCollectorSchedulingProperties properties = new OjCollectorSchedulingProperties(
                List.of(
                        new OjCollectorSchedulingProperties.Schedule(
                                "disabled",
                                "CODEFORCES",
                                false,
                                "0 0 1 * * *",
                                "UTC",
                                Duration.ofHours(24)
                        ),
                        new OjCollectorSchedulingProperties.Schedule(
                                "codeforces-daily-recent-submissions",
                                "CODEFORCES",
                                true,
                                "0 0 12 * * *",
                                "Asia/Shanghai",
                                Duration.ofHours(120)
                        ),
                        new OjCollectorSchedulingProperties.Schedule(
                                "atcoder-daily-recent-submissions",
                                "ATCODER",
                                true,
                                "0 30 12 * * *",
                                "Asia/Shanghai",
                                Duration.ofHours(48)
                        )
                ),
                null
        );
        ScheduledTaskRegistrar registrar = new ScheduledTaskRegistrar();

        new OjCollectorSchedulingConfig(
                properties,
                collectionService,
                new OjWarehouseRefreshDispatcher(List.of(codeforcesRefreshHandler, atcoderRefreshHandler))
        ).configureTasks(registrar);

        assertThat(registrar.getTriggerTaskList()).hasSize(2);
        registrar.getTriggerTaskList().getFirst().getRunnable().run();
        registrar.getTriggerTaskList().get(1).getRunnable().run();
        verify(collectionService).collectRecentWindowForConfiguredHandles("CODEFORCES", Duration.ofHours(120));
        verify(collectionService).collectRecentWindowForConfiguredHandles("ATCODER", Duration.ofHours(48));
        verify(codeforcesRefreshHandler).refresh("batch-codeforces");
        verify(atcoderRefreshHandler).refresh("batch-atcoder");
    }

    @Test
    void skipsWarehouseRefreshWhenScheduledCollectionReturnsNoBatch() throws Exception {
        OjScheduledSubmissionCollectionService collectionService = mock(OjScheduledSubmissionCollectionService.class);
        when(collectionService.collectRecentWindowForConfiguredHandles("CODEFORCES", Duration.ofHours(120)))
                .thenReturn(successResult("CODEFORCES", null));
        OjWarehouseRefreshHandler refreshHandler = refreshHandler("CODEFORCES", "batch-codeforces");
        OjCollectorSchedulingProperties properties = new OjCollectorSchedulingProperties(
                List.of(new OjCollectorSchedulingProperties.Schedule(
                        "daily-recent-submissions",
                        "CODEFORCES",
                        true,
                        "0 0 12 * * *",
                        "Asia/Shanghai",
                        Duration.ofHours(120)
                )),
                null
        );
        ScheduledTaskRegistrar registrar = new ScheduledTaskRegistrar();

        new OjCollectorSchedulingConfig(
                properties,
                collectionService,
                new OjWarehouseRefreshDispatcher(List.of(refreshHandler))
        ).configureTasks(registrar);

        registrar.getTriggerTaskList().getFirst().getRunnable().run();
        verify(refreshHandler).ojName();
        verify(refreshHandler, never()).refresh(any());
    }

    @Test
    void enablesScheduleByDefaultWhenFlagIsOmitted() {
        OjCollectorSchedulingProperties properties = new OjCollectorSchedulingProperties(
                List.of(new OjCollectorSchedulingProperties.Schedule(
                        "daily-recent-submissions",
                        "CODEFORCES",
                        null,
                        "0 0 12 * * *",
                        "Asia/Shanghai",
                        Duration.ofHours(120)
                )),
                null
        );
        ScheduledTaskRegistrar registrar = new ScheduledTaskRegistrar();

        new OjCollectorSchedulingConfig(
                properties,
                mock(OjScheduledSubmissionCollectionService.class),
                new OjWarehouseRefreshDispatcher(List.of())
        ).configureTasks(registrar);

        assertThat(registrar.getTriggerTaskList()).hasSize(1);
        assertThat(properties.jobItemInterval()).isEqualTo(Duration.ofSeconds(4));
    }

    @Test
    void usesConfiguredJobItemInterval() {
        OjCollectorSchedulingProperties properties = new OjCollectorSchedulingProperties(
                List.of(),
                Duration.ofSeconds(2)
        );

        assertThat(properties.jobItemInterval()).isEqualTo(Duration.ofSeconds(2));
    }

    @Test
    void doesNotRegisterTasksWhenNoScheduleIsEnabled() {
        OjCollectorSchedulingProperties properties = new OjCollectorSchedulingProperties(
                List.of(new OjCollectorSchedulingProperties.Schedule(
                        "daily-recent-submissions",
                        "CODEFORCES",
                        false,
                        "0 0 12 * * *",
                        "Asia/Shanghai",
                        Duration.ofHours(120)
                )),
                null
        );
        ScheduledTaskRegistrar registrar = new ScheduledTaskRegistrar();

        new OjCollectorSchedulingConfig(
                properties,
                mock(OjScheduledSubmissionCollectionService.class),
                new OjWarehouseRefreshDispatcher(List.of())
        ).configureTasks(registrar);

        assertThat(registrar.getTriggerTaskList()).isEmpty();
    }

    private OjWarehouseRefreshHandler refreshHandler(String ojName, String batchId) {
        OjWarehouseRefreshHandler handler = mock(OjWarehouseRefreshHandler.class);
        when(handler.ojName()).thenReturn(ojName);
        when(handler.refresh(batchId)).thenReturn(new OjSubmissionCollectionJobRefreshResult(
                OjSubmissionCollectionJobRefreshStatus.SUCCESS,
                "SUCCESS"
        ));
        return handler;
    }

    private OjSubmissionCollectionResult successResult(String ojName, String batchId) {
        return new OjSubmissionCollectionResult(
                ojName,
                OjSubmissionCollectionStatus.SUCCESS,
                Instant.parse("2026-06-30T04:00:00Z"),
                Instant.parse("2026-07-05T04:00:00Z"),
                0,
                0,
                0,
                0,
                0,
                batchId,
                null,
                0,
                null,
                "No Codeforces handles configured for collection",
                List.of()
        );
    }
}
