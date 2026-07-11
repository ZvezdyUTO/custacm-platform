package com.custacm.platform.trainingdata.common.app.warehouse;

import com.custacm.platform.common.sqltask.SqlTaskExecutionRequest;
import com.custacm.platform.common.sqltask.SqlTaskExecutionResult;
import com.custacm.platform.common.sqltask.SqlTaskRunStatus;
import com.custacm.platform.common.sqltask.SqlTaskRunner;
import com.custacm.platform.trainingdata.common.domain.oj.model.OjWarehouseRefreshInterval;
import com.custacm.platform.trainingdata.common.domain.oj.repo.OjWarehouseRefreshIntervalRepository;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.sql.Date;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class OjWarehouseRefreshServiceTest {
    private final SqlTaskRunner runner = mock(SqlTaskRunner.class);
    private final OjWarehouseRefreshIntervalRepository intervalRepository =
            mock(OjWarehouseRefreshIntervalRepository.class);
    private final OjWarehouseRefreshService service = new OjWarehouseRefreshService(
            runner,
            intervalRepository,
            " classpath:sql/tasks/example-warehouse-refresh.yml ",
            "batchId has no example submissions"
    );

    @Test
    void runsConfiguredManifestWithBatchIdDateParametersAndResumeNode() {
        OjWarehouseRefreshInterval interval = new OjWarehouseRefreshInterval(
                LocalDate.parse("2026-07-01"),
                LocalDate.parse("2026-07-03")
        );
        when(intervalRepository.findBatchDateInterval("batch-1")).thenReturn(Optional.of(interval));
        when(runner.execute(any())).thenReturn(successResult());

        SqlTaskExecutionResult result = service.refresh(
                " batch-1 ",
                " example.dwm.handle_problem_first_accepted "
        );

        ArgumentCaptor<SqlTaskExecutionRequest> captor = ArgumentCaptor.forClass(SqlTaskExecutionRequest.class);
        verify(runner).execute(captor.capture());
        SqlTaskExecutionRequest request = captor.getValue();
        assertThat(result.status()).isEqualTo(SqlTaskRunStatus.SUCCESS);
        assertThat(request.manifestLocation()).isEqualTo("classpath:sql/tasks/example-warehouse-refresh.yml");
        assertThat(request.parameters().get("batchId")).isEqualTo("batch-1");
        assertThat(request.parameters().get("refreshFromDateUtcPlus8"))
                .isEqualTo(Date.valueOf(LocalDate.parse("2026-07-01")));
        assertThat(request.parameters().get("refreshToDateUtcPlus8"))
                .isEqualTo(Date.valueOf(LocalDate.parse("2026-07-03")));
        assertThat(request.startFromTaskId()).isEqualTo("example.dwm.handle_problem_first_accepted");
    }

    @Test
    void refreshesLatestBatchWithTheSameStrictIntervalExecution() {
        OjWarehouseRefreshInterval interval = new OjWarehouseRefreshInterval(
                LocalDate.parse("2026-07-04"),
                LocalDate.parse("2026-07-06")
        );
        when(intervalRepository.findLatestBatchId()).thenReturn(Optional.of("batch-latest"));
        when(intervalRepository.findBatchDateInterval("batch-latest")).thenReturn(Optional.of(interval));
        when(runner.execute(any())).thenReturn(successResult());

        SqlTaskExecutionResult result = service.refreshLatest(" example.dws.daily_summary ");

        ArgumentCaptor<SqlTaskExecutionRequest> captor = ArgumentCaptor.forClass(SqlTaskExecutionRequest.class);
        verify(intervalRepository).findLatestBatchId();
        verify(intervalRepository).findBatchDateInterval("batch-latest");
        verify(runner).execute(captor.capture());
        assertThat(result.status()).isEqualTo(SqlTaskRunStatus.SUCCESS);
        assertThat(captor.getValue().parameters().get("batchId")).isEqualTo("batch-latest");
        assertThat(captor.getValue().startFromTaskId()).isEqualTo("example.dws.daily_summary");
    }

    @Test
    void rejectsLatestRefreshWhenOdsHasNoValidBatch() {
        when(intervalRepository.findLatestBatchId()).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.refreshLatest(null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("batchId has no example submissions");

        verify(intervalRepository).findLatestBatchId();
        verifyNoInteractions(runner);
    }

    @Test
    void rejectsBlankBatchIdBeforeCallingRunner() {
        assertThatThrownBy(() -> service.refresh(" ", null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("batchId must not be blank");

        verifyNoInteractions(runner, intervalRepository);
    }

    @Test
    void rejectsBatchWithoutRefreshIntervalBeforeCallingRunner() {
        when(intervalRepository.findBatchDateInterval("missing-batch")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.refresh("missing-batch", null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("batchId has no example submissions");

        verify(intervalRepository).findBatchDateInterval("missing-batch");
        verifyNoInteractions(runner);
    }

    private SqlTaskExecutionResult successResult() {
        Instant now = Instant.parse("2026-07-08T00:00:00Z");
        return new SqlTaskExecutionResult(
                "run-1",
                SqlTaskRunStatus.SUCCESS,
                "classpath:sql/tasks/example-warehouse-refresh.yml",
                null,
                null,
                now,
                now,
                0L,
                List.of()
        );
    }
}
