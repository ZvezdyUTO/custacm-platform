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
import static org.mockito.Mockito.times;
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

    @Test
    void retriesFailedBatchWithItsOriginalIntervalAndTheCompleteDag() {
        when(intervalRepository.findBatchDateInterval("batch-1")).thenReturn(Optional.of(interval("2026-07-01", "2026-07-10")));
        when(runner.execute(any())).thenReturn(failedResult(), successResult());

        assertThat(service.refresh("batch-1", null).status()).isEqualTo(SqlTaskRunStatus.FAILED);
        assertThat(service.refresh("batch-1", "example.dws.daily_summary").status()).isEqualTo(SqlTaskRunStatus.SUCCESS);

        ArgumentCaptor<SqlTaskExecutionRequest> requests = ArgumentCaptor.forClass(SqlTaskExecutionRequest.class);
        verify(runner, times(2)).execute(requests.capture());
        verify(intervalRepository).findBatchDateInterval("batch-1");
        assertThat(requests.getAllValues().get(1).parameters()).isEqualTo(requests.getAllValues().getFirst().parameters());
        assertThat(requests.getAllValues().get(1).startFromTaskId()).isNull();
        assertThat(service.refreshPending()).isEmpty();
    }

    @Test
    void carriesFailedIntervalsIntoTheNextBatchAndClearsThemOnlyAfterSuccess() {
        when(intervalRepository.findBatchDateInterval("batch-1")).thenReturn(Optional.of(interval("2026-07-01", "2026-07-10")));
        when(intervalRepository.findBatchDateInterval("batch-2")).thenReturn(Optional.of(interval("2026-08-01", "2026-08-01")));
        when(intervalRepository.findBatchDateInterval("batch-3")).thenReturn(Optional.of(interval("2026-09-01", "2026-09-01")));
        when(runner.execute(any())).thenReturn(failedResult(), failedResult(), successResult(), successResult());

        service.refresh("batch-1", null);
        service.refresh("batch-2", "example.dws.daily_summary");
        assertThat(service.refreshPending()).get().extracting(SqlTaskExecutionResult::status).isEqualTo(SqlTaskRunStatus.SUCCESS);
        service.refresh("batch-3", null);

        ArgumentCaptor<SqlTaskExecutionRequest> requests = ArgumentCaptor.forClass(SqlTaskExecutionRequest.class);
        verify(runner, times(4)).execute(requests.capture());
        for (SqlTaskExecutionRequest request : requests.getAllValues().subList(1, 3)) {
            assertThat(request.parameters().get("refreshFromDateUtcPlus8")).isEqualTo(Date.valueOf("2026-07-01"));
            assertThat(request.parameters().get("refreshToDateUtcPlus8")).isEqualTo(Date.valueOf("2026-08-01"));
            assertThat(request.startFromTaskId()).isNull();
        }
        assertThat(requests.getAllValues().get(3).parameters().get("refreshFromDateUtcPlus8"))
                .isEqualTo(Date.valueOf("2026-09-01"));
    }

    @Test
    void retainsPendingIntervalWhenExecutionThrowsAndRetriesWithoutNewOdsRows() {
        when(intervalRepository.findBatchDateInterval("batch-1")).thenReturn(Optional.of(interval("2026-07-01", "2026-07-10")));
        when(runner.execute(any())).thenThrow(new IllegalStateException("connection unavailable")).thenReturn(successResult());

        assertThatThrownBy(() -> service.refresh("batch-1", null)).hasMessage("connection unavailable");
        assertThat(service.refreshPending()).get().extracting(SqlTaskExecutionResult::status).isEqualTo(SqlTaskRunStatus.SUCCESS);
        assertThat(service.refreshPending()).isEmpty();

        verify(intervalRepository).findBatchDateInterval("batch-1");
        verify(runner, times(2)).execute(any());
    }

    @Test
    void missingNewBatchDoesNotDiscardEarlierPendingWork() {
        when(intervalRepository.findBatchDateInterval("batch-1")).thenReturn(Optional.of(interval("2026-07-01", "2026-07-10")));
        when(intervalRepository.findBatchDateInterval("missing")).thenReturn(Optional.empty());
        when(runner.execute(any())).thenReturn(failedResult(), successResult());
        service.refresh("batch-1", null);

        assertThatThrownBy(() -> service.refresh("missing", null)).isInstanceOf(IllegalArgumentException.class);
        assertThat(service.refreshPending()).isPresent();
        verify(runner, times(2)).execute(any());
    }

    @Test
    void doesNotQueryOrRunSqlWithoutPendingWork() {
        assertThat(service.refreshPending()).isEmpty();
        verifyNoInteractions(runner, intervalRepository);
    }

    private static OjWarehouseRefreshInterval interval(String from, String to) {
        return new OjWarehouseRefreshInterval(LocalDate.parse(from), LocalDate.parse(to));
    }

    private SqlTaskExecutionResult failedResult() {
        Instant now = Instant.parse("2026-07-08T00:00:00Z");
        return new SqlTaskExecutionResult("failed-run", SqlTaskRunStatus.FAILED,
                "classpath:sql/tasks/example-warehouse-refresh.yml", null, "example.dws.daily_summary",
                now, now, 0L, List.of());
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
