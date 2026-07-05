package com.custacm.platform.trainingdata.codeforces.scheduler;

import com.custacm.platform.trainingdata.codeforces.app.collector.result.CodeforcesSubmissionCollectionResult;
import com.custacm.platform.trainingdata.codeforces.app.collector.result.CodeforcesSubmissionCollectionStatus;
import com.custacm.platform.trainingdata.codeforces.app.collector.CodeforcesSubmissionCollectionService;
import com.custacm.platform.trainingdata.codeforces.collector.config.CodeforcesCollectorProperties;
import org.junit.jupiter.api.Test;
import org.springframework.scheduling.config.ScheduledTaskRegistrar;

import java.time.Duration;
import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class CodeforcesCollectorSchedulingConfigTest {
    @Test
    void registersOnlyEnabledCollectorSchedulesFromProperties() throws Exception {
        CodeforcesSubmissionCollectionService collectionService = mock(CodeforcesSubmissionCollectionService.class);
        when(collectionService.collectRecentWindowForConfiguredHandles(Duration.ofHours(120)))
                .thenReturn(successResult());
        CodeforcesCollectorProperties properties = new CodeforcesCollectorProperties(
                "https://codeforces.com",
                1000,
                Duration.ofSeconds(10),
                Duration.ofSeconds(30),
                Duration.ZERO,
                3,
                List.of(
                        new CodeforcesCollectorProperties.Schedule(
                                "disabled",
                                false,
                                "0 0 1 * * *",
                                "UTC",
                                Duration.ofHours(24)
                        ),
                        new CodeforcesCollectorProperties.Schedule(
                                "daily-recent-submissions",
                                true,
                                "0 0 12 * * *",
                                "Asia/Shanghai",
                                Duration.ofHours(120)
                        )
                )
        );
        ScheduledTaskRegistrar registrar = new ScheduledTaskRegistrar();

        new CodeforcesCollectorSchedulingConfig(properties, collectionService).configureTasks(registrar);

        assertThat(registrar.getTriggerTaskList()).hasSize(1);
        registrar.getTriggerTaskList().getFirst().getRunnable().run();
        verify(collectionService).collectRecentWindowForConfiguredHandles(Duration.ofHours(120));
    }

    @Test
    void doesNotRegisterTasksWhenNoScheduleIsEnabled() {
        CodeforcesCollectorProperties properties = new CodeforcesCollectorProperties(
                "https://codeforces.com",
                1000,
                Duration.ofSeconds(10),
                Duration.ofSeconds(30),
                Duration.ZERO,
                3,
                List.of(new CodeforcesCollectorProperties.Schedule(
                        "daily-recent-submissions",
                        false,
                        "0 0 12 * * *",
                        "Asia/Shanghai",
                        Duration.ofHours(120)
                ))
        );
        ScheduledTaskRegistrar registrar = new ScheduledTaskRegistrar();

        new CodeforcesCollectorSchedulingConfig(
                properties,
                mock(CodeforcesSubmissionCollectionService.class)
        ).configureTasks(registrar);

        assertThat(registrar.getTriggerTaskList()).isEmpty();
    }

    private CodeforcesSubmissionCollectionResult successResult() {
        return new CodeforcesSubmissionCollectionResult(
                CodeforcesSubmissionCollectionStatus.SUCCESS,
                Instant.parse("2026-06-30T04:00:00Z"),
                Instant.parse("2026-07-05T04:00:00Z"),
                0,
                0,
                0,
                0,
                0,
                null,
                null,
                0,
                null,
                "No Codeforces handles configured for collection",
                List.of()
        );
    }
}
