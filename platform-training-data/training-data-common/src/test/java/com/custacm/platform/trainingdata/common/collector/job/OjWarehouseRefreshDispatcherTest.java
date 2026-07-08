package com.custacm.platform.trainingdata.common.collector.job;

import com.custacm.platform.trainingdata.common.collector.result.OjSubmissionCollectionResult;
import com.custacm.platform.trainingdata.common.collector.result.OjSubmissionCollectionStatus;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class OjWarehouseRefreshDispatcherTest {
    @Test
    void dispatchesRefreshToMatchingOjHandler() {
        OjWarehouseRefreshDispatcher dispatcher = new OjWarehouseRefreshDispatcher(List.of(
                new FakeHandler("ATCODER", OjSubmissionCollectionJobRefreshResult.notRequested())
        ));

        OjSubmissionCollectionJobRefreshResult result = dispatcher.refresh(collectionResult("ATCODER", "batch-1"));

        assertThat(result.status()).isEqualTo(OjSubmissionCollectionJobRefreshStatus.NOT_REQUESTED);
    }

    @Test
    void normalizesOjNameBeforeDispatching() {
        OjWarehouseRefreshDispatcher dispatcher = new OjWarehouseRefreshDispatcher(List.of(
                new FakeHandler("atcoder", new OjSubmissionCollectionJobRefreshResult(
                        OjSubmissionCollectionJobRefreshStatus.SUCCESS,
                        "SUCCESS"
                ))
        ));

        OjSubmissionCollectionJobRefreshResult result = dispatcher.refresh(collectionResult(" ATCODER ", "batch-1"));

        assertThat(result.status()).isEqualTo(OjSubmissionCollectionJobRefreshStatus.SUCCESS);
        assertThat(result.message()).isEqualTo("SUCCESS");
    }

    @Test
    void returnsFailedRefreshForUnsupportedOj() {
        OjWarehouseRefreshDispatcher dispatcher = new OjWarehouseRefreshDispatcher(List.of());

        OjSubmissionCollectionJobRefreshResult result = dispatcher.refresh(collectionResult("ATCODER", "batch-1"));

        assertThat(result.status()).isEqualTo(OjSubmissionCollectionJobRefreshStatus.FAILED);
        assertThat(result.message()).isEqualTo("ATCODER warehouse refresh is not implemented");
    }

    @Test
    void rejectsDuplicateHandlers() {
        assertThatThrownBy(() -> new OjWarehouseRefreshDispatcher(List.of(
                new FakeHandler("ATCODER", OjSubmissionCollectionJobRefreshResult.notRequested()),
                new FakeHandler("atcoder", OjSubmissionCollectionJobRefreshResult.notRequested())
        ))).isInstanceOf(IllegalArgumentException.class)
                .hasMessage("duplicate OJ warehouse refresh handler: ATCODER");
    }

    @Test
    void rejectsBlankHandlerOjName() {
        assertThatThrownBy(() -> new OjWarehouseRefreshDispatcher(List.of(
                new FakeHandler(" ", OjSubmissionCollectionJobRefreshResult.notRequested())
        ))).isInstanceOf(IllegalArgumentException.class)
                .hasMessage("oj name must not be blank");
    }

    private static OjSubmissionCollectionResult collectionResult(String ojName, String batchId) {
        Instant now = Instant.parse("2026-07-08T00:00:00Z");
        return new OjSubmissionCollectionResult(
                ojName,
                OjSubmissionCollectionStatus.SUCCESS,
                now.minusSeconds(3600),
                now,
                1,
                1,
                0,
                1,
                1,
                batchId,
                "ods_atcoder__submission",
                1,
                now,
                null,
                List.of()
        );
    }

    private record FakeHandler(
            String ojName,
            OjSubmissionCollectionJobRefreshResult refreshResult
    ) implements OjWarehouseRefreshHandler {
        @Override
        public OjSubmissionCollectionJobRefreshResult refresh(String batchId) {
            return refreshResult;
        }
    }
}
