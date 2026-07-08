package com.custacm.platform.trainingdata.common.collector.job;

import com.custacm.platform.common.sqltask.SqlTaskExecutionResult;
import com.custacm.platform.common.sqltask.SqlTaskRunStatus;
import com.custacm.platform.trainingdata.common.app.warehouse.OjWarehouseRefreshService;
import com.custacm.platform.trainingdata.common.domain.oj.value.OjNames;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class SqlTaskOjWarehouseRefreshHandlerTest {
    private final OjWarehouseRefreshService refreshService = mock(OjWarehouseRefreshService.class);
    private final SqlTaskOjWarehouseRefreshHandler handler =
            new SqlTaskOjWarehouseRefreshHandler(" codeforces ", refreshService);

    @Test
    void mapsSuccessfulSqlTaskRunToSuccessfulJobRefresh() {
        when(refreshService.refresh("batch-1", null)).thenReturn(sqlResult(SqlTaskRunStatus.SUCCESS));

        OjSubmissionCollectionJobRefreshResult result = handler.refresh("batch-1");

        assertThat(handler.ojName()).isEqualTo(OjNames.CODEFORCES);
        assertThat(result.status()).isEqualTo(OjSubmissionCollectionJobRefreshStatus.SUCCESS);
        assertThat(result.message()).isEqualTo("SUCCESS");
        verify(refreshService).refresh("batch-1", null);
    }

    @Test
    void mapsFailedSqlTaskRunToFailedJobRefresh() {
        when(refreshService.refresh("batch-1", null)).thenReturn(sqlResult(SqlTaskRunStatus.FAILED));

        OjSubmissionCollectionJobRefreshResult result = handler.refresh("batch-1");

        assertThat(result.status()).isEqualTo(OjSubmissionCollectionJobRefreshStatus.FAILED);
        assertThat(result.message()).isEqualTo("FAILED");
    }

    @Test
    void delegatesBlankBatchIdValidationToRefreshService() {
        when(refreshService.refresh(" ", null)).thenThrow(new IllegalArgumentException("batchId must not be blank"));

        assertThatThrownBy(() -> handler.refresh(" "))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("batchId must not be blank");
    }

    private SqlTaskExecutionResult sqlResult(SqlTaskRunStatus status) {
        Instant now = Instant.parse("2026-07-08T00:00:00Z");
        return new SqlTaskExecutionResult(
                "run-1",
                status,
                "classpath:sql/tasks/example-warehouse-refresh.yml",
                null,
                status == SqlTaskRunStatus.SUCCESS ? null : "example.dwd.submission",
                now,
                now,
                0L,
                List.of()
        );
    }
}
