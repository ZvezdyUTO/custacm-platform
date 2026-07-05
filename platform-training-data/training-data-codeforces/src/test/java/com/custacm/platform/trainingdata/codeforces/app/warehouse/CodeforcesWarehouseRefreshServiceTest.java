package com.custacm.platform.trainingdata.codeforces.app.warehouse;

import com.custacm.platform.common.sqltask.SqlTaskExecutionRequest;
import com.custacm.platform.common.sqltask.SqlTaskExecutionResult;
import com.custacm.platform.common.sqltask.SqlTaskRunStatus;
import com.custacm.platform.common.sqltask.SqlTaskRunner;
import com.custacm.platform.trainingdata.codeforces.domain.model.CodeforcesWarehouseRefreshInterval;
import com.custacm.platform.trainingdata.codeforces.domain.repo.CodeforcesWarehouseRefreshIntervalRepository;
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

class CodeforcesWarehouseRefreshServiceTest {
    private final SqlTaskRunner runner = mock(SqlTaskRunner.class);
    private final CodeforcesWarehouseRefreshIntervalRepository intervalRepository =
            mock(CodeforcesWarehouseRefreshIntervalRepository.class);
    private final CodeforcesWarehouseRefreshService service =
            new CodeforcesWarehouseRefreshService(runner, intervalRepository);

    @Test
    void runsCodeforcesManifestWithBatchIdParameterAndResumeNode() {
        CodeforcesWarehouseRefreshInterval interval = interval("2024-01-05", "2024-01-06");
        when(intervalRepository.findBatchDateInterval("batch-1")).thenReturn(Optional.of(interval));
        when(runner.execute(any())).thenReturn(successResult());

        SqlTaskExecutionResult result = service.refresh(
                " batch-1 ",
                " codeforces.dwm.handle_problem_first_accepted "
        );

        ArgumentCaptor<SqlTaskExecutionRequest> captor = ArgumentCaptor.forClass(SqlTaskExecutionRequest.class);
        verify(runner).execute(captor.capture());
        SqlTaskExecutionRequest request = captor.getValue();
        assertThat(result.status()).isEqualTo(SqlTaskRunStatus.SUCCESS);
        assertThat(request.manifestLocation()).isEqualTo("classpath:sql/tasks/codeforces-warehouse-refresh.yml");
        assertThat(request.parameters().get("batchId")).isEqualTo("batch-1");
        assertThat(request.parameters().get("refreshFromDateUtcPlus8"))
                .isEqualTo(Date.valueOf(LocalDate.parse("2024-01-05")));
        assertThat(request.parameters().get("refreshToDateUtcPlus8"))
                .isEqualTo(Date.valueOf(LocalDate.parse("2024-01-06")));
        assertThat(request.startFromTaskId()).isEqualTo("codeforces.dwm.handle_problem_first_accepted");
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
                .hasMessage("batchId has no Codeforces submissions with creationTimeSeconds");

        verify(intervalRepository).findBatchDateInterval("missing-batch");
        verifyNoInteractions(runner);
    }

    private CodeforcesWarehouseRefreshInterval interval(String fromDate, String toDate) {
        return new CodeforcesWarehouseRefreshInterval(
                LocalDate.parse(fromDate),
                LocalDate.parse(toDate)
        );
    }

    private SqlTaskExecutionResult successResult() {
        Instant now = Instant.parse("2026-07-05T00:00:00Z");
        return new SqlTaskExecutionResult(
                "run-1",
                SqlTaskRunStatus.SUCCESS,
                "classpath:sql/tasks/codeforces-warehouse-refresh.yml",
                null,
                null,
                now,
                now,
                0L,
                List.of()
        );
    }
}
