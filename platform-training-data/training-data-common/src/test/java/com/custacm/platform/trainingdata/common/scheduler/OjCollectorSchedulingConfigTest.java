package com.custacm.platform.trainingdata.common.scheduler;

import com.custacm.platform.trainingdata.common.collector.OjCollectionExecutionCoordinator;
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
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

import static java.util.concurrent.TimeUnit.SECONDS;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class OjCollectorSchedulingConfigTest {
    @Test
    void registersOnlyEnabledCollectorSchedulesFromPropertiesAndPassesOjName() throws Exception {
        OjScheduledSubmissionCollectionService collectionService = mock(OjScheduledSubmissionCollectionService.class);
        when(collectionService.collectRecentWindowForConfiguredHandles("CODEFORCES", Duration.ofHours(120)))
                .thenReturn(successResult("CODEFORCES", "batch-codeforces"));
        when(collectionService.collectRecentWindowForConfiguredHandles("ATCODER", Duration.ZERO))
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
                                Duration.ZERO
                        )
                ),
                null
        );
        ScheduledTaskRegistrar registrar = new ScheduledTaskRegistrar();

        new OjCollectorSchedulingConfig(
                properties,
                collectionService,
                new OjWarehouseRefreshDispatcher(List.of(codeforcesRefreshHandler, atcoderRefreshHandler)),
                Runnable::run,
                new OjCollectionExecutionCoordinator()
        ).configureTasks(registrar);

        assertThat(registrar.getTriggerTaskList()).hasSize(2);
        registrar.getTriggerTaskList().getFirst().getRunnable().run();
        registrar.getTriggerTaskList().get(1).getRunnable().run();
        verify(collectionService).collectRecentWindowForConfiguredHandles("CODEFORCES", Duration.ofHours(120));
        verify(collectionService).collectRecentWindowForConfiguredHandles("ATCODER", Duration.ZERO);
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
                new OjWarehouseRefreshDispatcher(List.of(refreshHandler)),
                Runnable::run,
                new OjCollectionExecutionCoordinator()
        ).configureTasks(registrar);

        registrar.getTriggerTaskList().getFirst().getRunnable().run();
        verify(refreshHandler).ojName();
        verify(refreshHandler, never()).refresh(any());
    }

    @Test
    void skipsSameOjTriggerInsteadOfQueueingItBehindBusyWorkers() throws Exception {
        OjScheduledSubmissionCollectionService collectionService = mock(OjScheduledSubmissionCollectionService.class);
        CountDownLatch started = new CountDownLatch(2);
        CountDownLatch release = new CountDownLatch(1);
        AtomicInteger codeforcesRuns = new AtomicInteger();
        when(collectionService.collectRecentWindowForConfiguredHandles(any(), any())).thenAnswer(invocation -> {
            String ojName = invocation.getArgument(0);
            if ("CODEFORCES".equals(ojName)) {
                codeforcesRuns.incrementAndGet();
            }
            started.countDown();
            release.await();
            return successResult(ojName, null);
        });
        OjCollectorSchedulingProperties properties = new OjCollectorSchedulingProperties(
                List.of(
                        new OjCollectorSchedulingProperties.Schedule(
                                "codeforces",
                                "CODEFORCES",
                                true,
                                "0 0 12 * * *",
                                "UTC",
                                Duration.ZERO
                        ),
                        new OjCollectorSchedulingProperties.Schedule(
                                "atcoder",
                                "ATCODER",
                                true,
                                "0 0 12 * * *",
                                "UTC",
                                Duration.ZERO
                        )
                ),
                null
        );
        ScheduledTaskRegistrar registrar = new ScheduledTaskRegistrar();
        ExecutorService executor = Executors.newFixedThreadPool(2);
        new OjCollectorSchedulingConfig(
                properties,
                collectionService,
                new OjWarehouseRefreshDispatcher(List.of()),
                executor,
                new OjCollectionExecutionCoordinator()
        ).configureTasks(registrar);

        try {
            registrar.getTriggerTaskList().getFirst().getRunnable().run();
            registrar.getTriggerTaskList().get(1).getRunnable().run();
            assertThat(started.await(2, SECONDS)).isTrue();

            registrar.getTriggerTaskList().getFirst().getRunnable().run();
            release.countDown();
            executor.shutdown();
            assertThat(executor.awaitTermination(2, SECONDS)).isTrue();

            assertThat(codeforcesRuns).hasValue(1);
            verify(collectionService, times(1))
                    .collectRecentWindowForConfiguredHandles("CODEFORCES", Duration.ZERO);
        } finally {
            release.countDown();
            executor.shutdownNow();
        }
    }

    @Test
    void keepsScheduleDisabledWhenFlagIsOmitted() {
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
                new OjWarehouseRefreshDispatcher(List.of()),
                Runnable::run,
                new OjCollectionExecutionCoordinator()
        ).configureTasks(registrar);

        assertThat(registrar.getTriggerTaskList()).isEmpty();
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
                new OjWarehouseRefreshDispatcher(List.of()),
                Runnable::run,
                new OjCollectionExecutionCoordinator()
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
