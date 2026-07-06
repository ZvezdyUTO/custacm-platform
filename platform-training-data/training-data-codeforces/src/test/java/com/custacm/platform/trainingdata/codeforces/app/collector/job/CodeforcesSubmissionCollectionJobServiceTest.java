package com.custacm.platform.trainingdata.codeforces.app.collector.job;

import com.custacm.platform.common.sqltask.SqlTaskExecutionResult;
import com.custacm.platform.common.sqltask.SqlTaskRunStatus;
import com.custacm.platform.trainingdata.codeforces.app.collector.CodeforcesSubmissionCollectionService;
import com.custacm.platform.trainingdata.codeforces.app.collector.result.CodeforcesSubmissionCollectionHandleResult;
import com.custacm.platform.trainingdata.codeforces.app.collector.result.CodeforcesSubmissionCollectionResult;
import com.custacm.platform.trainingdata.codeforces.app.collector.result.CodeforcesSubmissionCollectionStatus;
import com.custacm.platform.trainingdata.codeforces.app.warehouse.CodeforcesWarehouseRefreshService;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.concurrent.Executor;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class CodeforcesSubmissionCollectionJobServiceTest {
    private static final Instant NOW = Instant.parse("2026-07-06T03:00:00Z");

    private final CodeforcesSubmissionCollectionService collectionService =
            mock(CodeforcesSubmissionCollectionService.class);
    private final CodeforcesWarehouseRefreshService refreshService = mock(CodeforcesWarehouseRefreshService.class);

    @Test
    void startsJobAndAggregatesCollectionAndRefreshResults() throws Exception {
        CodeforcesSubmissionCollectionJobService service = service(Runnable::run);
        when(collectionService.collectRecentWindowForStudentIdentity("230511213黄炳睿", Duration.ofHours(24)))
                .thenReturn(collectionResult("tourist", "batch-1", 10));
        when(refreshService.refresh("batch-1", null))
                .thenReturn(refreshResult(SqlTaskRunStatus.SUCCESS));

        CodeforcesSubmissionCollectionJobSnapshot snapshot = service.startBatchCollection(
                List.of("230511213黄炳睿"),
                Duration.ofHours(24),
                true
        );

        assertThat(snapshot.status()).isEqualTo(CodeforcesSubmissionCollectionJobStatus.SUCCESS);
        assertThat(snapshot.requestedCount()).isEqualTo(1);
        assertThat(snapshot.completedCount()).isEqualTo(1);
        assertThat(snapshot.collectedCount()).isEqualTo(1);
        assertThat(snapshot.refreshedCount()).isEqualTo(1);
        assertThat(snapshot.writtenRows()).isEqualTo(10);
        assertThat(snapshot.batchIds()).containsExactly("batch-1");
        assertThat(snapshot.items()).singleElement().satisfies(item -> {
            assertThat(item.studentIdentity()).isEqualTo("230511213黄炳睿");
            assertThat(item.itemStatus()).isEqualTo(CodeforcesSubmissionCollectionJobItemStatus.SUCCESS);
            assertThat(item.handle()).isEqualTo("tourist");
            assertThat(item.refreshStatus()).isEqualTo(CodeforcesSubmissionCollectionJobRefreshStatus.SUCCESS);
        });
        verify(collectionService).collectRecentWindowForStudentIdentity("230511213黄炳睿", Duration.ofHours(24));
        verify(refreshService).refresh("batch-1", null);
    }

    @Test
    void returnsActiveJobWhenAnotherBatchIsAlreadyRunning() throws Exception {
        List<Runnable> queued = new ArrayList<>();
        CodeforcesSubmissionCollectionJobService service = service(queued::add);

        CodeforcesSubmissionCollectionJobSnapshot first = service.startBatchCollection(
                List.of("230511213黄炳睿"),
                Duration.ofHours(24),
                false
        );
        CodeforcesSubmissionCollectionJobSnapshot second = service.startBatchCollection(
                List.of("230511214李明"),
                Duration.ofHours(24),
                false
        );

        assertThat(first.status()).isEqualTo(CodeforcesSubmissionCollectionJobStatus.RUNNING);
        assertThat(second.jobId()).isEqualTo(first.jobId());
        assertThat(queued).hasSize(1);
        verify(collectionService, never()).collectRecentWindowForStudentIdentity("230511214李明", Duration.ofHours(24));
    }

    @Test
    void listsRetainedJobsNewestFirst() throws Exception {
        CodeforcesSubmissionCollectionJobService service = service(Runnable::run);
        when(collectionService.collectRecentWindowForStudentIdentity("230511213黄炳睿", Duration.ofHours(24)))
                .thenReturn(collectionResult("tourist", "batch-1", 10));

        CodeforcesSubmissionCollectionJobSnapshot started = service.startBatchCollection(
                List.of("230511213黄炳睿"),
                Duration.ofHours(24),
                false
        );

        assertThat(service.listJobs())
                .extracting(CodeforcesSubmissionCollectionJobSnapshot::jobId)
                .containsExactly(started.jobId());
    }

    @Test
    void waitsBetweenIdentityCollections() throws Exception {
        List<String> events = new ArrayList<>();
        CodeforcesSubmissionCollectionJobService service = new CodeforcesSubmissionCollectionJobService(
                collectionService,
                refreshService,
                Runnable::run,
                Clock.fixed(NOW, ZoneOffset.UTC),
                Duration.ofSeconds(4),
                duration -> events.add("sleep:" + duration)
        );
        when(collectionService.collectRecentWindowForStudentIdentity("230511213黄炳睿", Duration.ofHours(24)))
                .thenAnswer(invocation -> {
                    events.add("collect:230511213黄炳睿");
                    return collectionResult("tourist", null, 0);
                });
        when(collectionService.collectRecentWindowForStudentIdentity("230511214李明", Duration.ofHours(24)))
                .thenAnswer(invocation -> {
                    events.add("collect:230511214李明");
                    return collectionResult("benq", null, 0);
                });

        service.startBatchCollection(
                List.of("230511213黄炳睿", "230511214李明"),
                Duration.ofHours(24),
                false
        );

        assertThat(events).containsExactly(
                "collect:230511213黄炳睿",
                "sleep:PT4S",
                "collect:230511214李明"
        );
    }

    @Test
    void rejectsMissingJob() {
        CodeforcesSubmissionCollectionJobService service = service(Runnable::run);

        assertThatThrownBy(() -> service.getJob("missing"))
                .isInstanceOf(NoSuchElementException.class)
                .hasMessageContaining("missing");
    }

    private CodeforcesSubmissionCollectionJobService service(Executor executor) {
        return new CodeforcesSubmissionCollectionJobService(
                collectionService,
                refreshService,
                executor,
                Clock.fixed(NOW, ZoneOffset.UTC)
        );
    }

    private static CodeforcesSubmissionCollectionResult collectionResult(
            String handle,
            String batchId,
            int writtenRows
    ) {
        return new CodeforcesSubmissionCollectionResult(
                CodeforcesSubmissionCollectionStatus.SUCCESS,
                NOW.minus(Duration.ofHours(24)),
                NOW,
                1,
                1,
                0,
                12,
                writtenRows,
                batchId,
                "ods_codeforces__submission",
                writtenRows,
                NOW,
                null,
                List.of(CodeforcesSubmissionCollectionHandleResult.success(handle, 12, writtenRows))
        );
    }

    private static SqlTaskExecutionResult refreshResult(SqlTaskRunStatus status) {
        return new SqlTaskExecutionResult(
                "run-1",
                status,
                "classpath:sql/tasks/codeforces-warehouse-refresh.yml",
                null,
                null,
                NOW,
                NOW,
                1,
                List.of()
        );
    }
}
