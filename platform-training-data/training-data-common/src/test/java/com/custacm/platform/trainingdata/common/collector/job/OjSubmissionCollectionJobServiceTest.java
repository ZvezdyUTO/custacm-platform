package com.custacm.platform.trainingdata.common.collector.job;

import com.custacm.platform.trainingdata.common.collector.result.OjSubmissionCollectionHandleResult;
import com.custacm.platform.trainingdata.common.collector.result.OjSubmissionCollectionResult;
import com.custacm.platform.trainingdata.common.collector.result.OjSubmissionCollectionStatus;
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

class OjSubmissionCollectionJobServiceTest {
    private static final Instant NOW = Instant.parse("2026-07-06T03:00:00Z");

    @Test
    void startsJobAndAggregatesCollectionAndRefreshResults() {
        List<String> events = new ArrayList<>();
        OjSubmissionCollectionJobService service = service(
                (ojName, studentIdentity, lookback) -> {
                    events.add("collect:" + ojName + ":" + studentIdentity + ":" + lookback);
                    return collectionResult("tourist", "batch-1", 10);
                },
                result -> {
                    events.add("refresh:" + result.batchId());
                    return new OjSubmissionCollectionJobRefreshResult(
                            OjSubmissionCollectionJobRefreshStatus.SUCCESS,
                            "SUCCESS"
                    );
                },
                Runnable::run
        );

        OjSubmissionCollectionJobSnapshot snapshot = service.startBatchCollection(
                List.of("230511213黄炳睿"),
                Duration.ofHours(24),
                true,
                "CODEFORCES"
        );

        assertThat(snapshot.ojName()).isEqualTo("CODEFORCES");
        assertThat(snapshot.status()).isEqualTo(OjSubmissionCollectionJobStatus.SUCCESS);
        assertThat(snapshot.requestedCount()).isEqualTo(1);
        assertThat(snapshot.completedCount()).isEqualTo(1);
        assertThat(snapshot.collectedCount()).isEqualTo(1);
        assertThat(snapshot.refreshedCount()).isEqualTo(1);
        assertThat(snapshot.writtenRows()).isEqualTo(10);
        assertThat(snapshot.batchIds()).containsExactly("batch-1");
        assertThat(snapshot.items()).singleElement().satisfies(item -> {
            assertThat(item.studentIdentity()).isEqualTo("230511213黄炳睿");
            assertThat(item.ojName()).isEqualTo("CODEFORCES");
            assertThat(item.itemStatus()).isEqualTo(OjSubmissionCollectionJobItemStatus.SUCCESS);
            assertThat(item.handle()).isEqualTo("tourist");
            assertThat(item.refreshStatus()).isEqualTo(OjSubmissionCollectionJobRefreshStatus.SUCCESS);
        });
        assertThat(events).containsExactly(
                "collect:CODEFORCES:230511213黄炳睿:PT24H",
                "refresh:batch-1"
        );
    }

    @Test
    void returnsActiveJobWhenAnotherBatchIsAlreadyRunning() {
        List<Runnable> queued = new ArrayList<>();
        OjSubmissionCollectionJobService service = service(
                (ojName, studentIdentity, lookback) -> collectionResult("tourist", "batch-1", 10),
                result -> OjSubmissionCollectionJobRefreshResult.notRequested(),
                queued::add
        );

        OjSubmissionCollectionJobSnapshot first = service.startBatchCollection(
                List.of("230511213黄炳睿"),
                Duration.ofHours(24),
                false
        );
        OjSubmissionCollectionJobSnapshot second = service.startBatchCollection(
                List.of("230511214李明"),
                Duration.ofHours(24),
                false
        );

        assertThat(first.status()).isEqualTo(OjSubmissionCollectionJobStatus.RUNNING);
        assertThat(second.jobId()).isEqualTo(first.jobId());
        assertThat(queued).hasSize(1);
    }

    @Test
    void listsRetainedJobsNewestFirst() {
        OjSubmissionCollectionJobService service = service(
                (ojName, studentIdentity, lookback) -> collectionResult("tourist", "batch-1", 10),
                result -> OjSubmissionCollectionJobRefreshResult.notRequested(),
                Runnable::run
        );

        OjSubmissionCollectionJobSnapshot started = service.startBatchCollection(
                List.of("230511213黄炳睿"),
                Duration.ofHours(24),
                false
        );

        assertThat(service.listJobs())
                .extracting(OjSubmissionCollectionJobSnapshot::jobId)
                .containsExactly(started.jobId());
    }

    @Test
    void waitsBetweenIdentityCollections() {
        List<String> events = new ArrayList<>();
        OjSubmissionCollectionJobService service = new OjSubmissionCollectionJobService(
                (ojName, studentIdentity, lookback) -> {
                    events.add("collect:" + ojName + ":" + studentIdentity);
                    return collectionResult(studentIdentity, null, 0);
                },
                result -> OjSubmissionCollectionJobRefreshResult.notRequested(),
                Runnable::run,
                Clock.fixed(NOW, ZoneOffset.UTC),
                Duration.ofSeconds(4),
                duration -> events.add("sleep:" + duration)
        );

        service.startBatchCollection(
                List.of("230511213黄炳睿", "230511214李明"),
                Duration.ofHours(24),
                false
        );

        assertThat(events).containsExactly(
                "collect:null:230511213黄炳睿",
                "sleep:PT4S",
                "collect:null:230511214李明"
        );
    }

    @Test
    void recordsNoBatchWhenRefreshIsRequestedWithoutWrittenBatch() {
        OjSubmissionCollectionJobService service = service(
                (ojName, studentIdentity, lookback) -> collectionResult("tourist", null, 0),
                result -> {
                    throw new AssertionError("refresh handler should not run without batch id");
                },
                Runnable::run
        );

        OjSubmissionCollectionJobSnapshot snapshot = service.startBatchCollection(
                List.of("230511213黄炳睿"),
                Duration.ofHours(24),
                true
        );

        assertThat(snapshot.items()).singleElement()
                .extracting(OjSubmissionCollectionJobItem::refreshStatus)
                .isEqualTo(OjSubmissionCollectionJobRefreshStatus.NO_BATCH);
    }

    @Test
    void recordsRefreshFailureWithoutFailingCollectionItem() {
        OjSubmissionCollectionJobService service = service(
                (ojName, studentIdentity, lookback) -> collectionResult("tourist", "batch-1", 10),
                result -> {
                    throw new IllegalStateException("refresh failed");
                },
                Runnable::run
        );

        OjSubmissionCollectionJobSnapshot snapshot = service.startBatchCollection(
                List.of("230511213黄炳睿"),
                Duration.ofHours(24),
                true
        );

        assertThat(snapshot.status()).isEqualTo(OjSubmissionCollectionJobStatus.SUCCESS);
        assertThat(snapshot.refreshedCount()).isZero();
        assertThat(snapshot.items()).singleElement().satisfies(item -> {
            assertThat(item.itemStatus()).isEqualTo(OjSubmissionCollectionJobItemStatus.SUCCESS);
            assertThat(item.refreshStatus()).isEqualTo(OjSubmissionCollectionJobRefreshStatus.FAILED);
            assertThat(item.refreshMessage()).isEqualTo("refresh failed");
        });
    }

    @Test
    void rejectsMissingJob() {
        OjSubmissionCollectionJobService service = service(
                (ojName, studentIdentity, lookback) -> collectionResult("tourist", "batch-1", 10),
                result -> OjSubmissionCollectionJobRefreshResult.notRequested(),
                Runnable::run
        );

        assertThatThrownBy(() -> service.getJob("missing"))
                .isInstanceOf(NoSuchElementException.class)
                .hasMessageContaining("missing");
    }

    private static OjSubmissionCollectionJobService service(
            OjSubmissionCollectionJobService.RecentIdentityCollector collector,
            OjSubmissionCollectionJobService.RefreshHandler refreshHandler,
            Executor executor
    ) {
        return new OjSubmissionCollectionJobService(
                collector,
                refreshHandler,
                executor,
                Clock.fixed(NOW, ZoneOffset.UTC)
        );
    }

    private static OjSubmissionCollectionResult collectionResult(
            String handle,
            String batchId,
            int writtenRows
    ) {
        return new OjSubmissionCollectionResult(
                "CODEFORCES",
                OjSubmissionCollectionStatus.SUCCESS,
                NOW.minus(Duration.ofHours(24)),
                NOW,
                1,
                1,
                0,
                12,
                writtenRows,
                batchId,
                batchId == null ? null : "ods_codeforces__submission",
                writtenRows,
                writtenRows == 0 ? null : NOW,
                null,
                List.of(OjSubmissionCollectionHandleResult.success(handle, 12, writtenRows))
        );
    }
}
