package com.custacm.platform.trainingdata.codeforces.web.warehouse;

import com.custacm.platform.common.sqltask.SqlTaskErrorCode;
import com.custacm.platform.common.sqltask.SqlTaskException;
import com.custacm.platform.common.sqltask.SqlTaskExecutionResult;
import com.custacm.platform.common.sqltask.SqlTaskNodeResult;
import com.custacm.platform.common.sqltask.SqlTaskRunStatus;
import com.custacm.platform.trainingdata.codeforces.app.warehouse.CodeforcesWarehouseRefreshService;
import com.custacm.platform.trainingdata.codeforces.web.warehouse.CodeforcesWarehouseRefreshController;
import com.custacm.platform.trainingdata.codeforces.web.warehouse.request.CodeforcesWarehouseRefreshRequest;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class CodeforcesWarehouseRefreshControllerTest {
    private static final Instant NOW = Instant.parse("2026-07-05T00:00:00Z");

    private final CodeforcesWarehouseRefreshService service = mock(CodeforcesWarehouseRefreshService.class);
    private final CodeforcesWarehouseRefreshController controller = new CodeforcesWarehouseRefreshController(service);

    @Test
    void refreshesWarehouseAndReturnsCoreExecutionResult() {
        when(service.refresh(" batch-1 ", " codeforces.dwm.handle_problem_first_accepted "))
                .thenReturn(result());

        SqlTaskExecutionResult result = controller.refresh(new CodeforcesWarehouseRefreshRequest(
                " batch-1 ",
                " codeforces.dwm.handle_problem_first_accepted "
        ));

        assertThat(result.runId()).isEqualTo("run-1");
        assertThat(result.status()).isEqualTo(SqlTaskRunStatus.SUCCESS);
        assertThat(result.tasks()).hasSize(1);
        assertThat(result.tasks().getFirst().taskId()).isEqualTo("codeforces.dwd.submission");
        verify(service).refresh(" batch-1 ", " codeforces.dwm.handle_problem_first_accepted ");
    }

    @Test
    void rejectsEmptyRequestBody() {
        assertThatThrownBy(() -> controller.refresh(null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("request body must not be empty");
    }

    @Test
    void mapsRefreshErrorsToHttpStatuses() {
        CodeforcesWarehouseRefreshExceptionHandler handler = new CodeforcesWarehouseRefreshExceptionHandler();

        var invalidRequest = handler.handleIllegalArgumentException(new IllegalArgumentException("bad request"));
        assertThat(invalidRequest.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(invalidRequest.getBody()).isEqualTo(Map.of(
                "code", "CODEFORCES_WAREHOUSE_REFRESH_INVALID_REQUEST",
                "message", "bad request"
        ));

        var invalidStartNode = handler.handleSqlTaskException(new SqlTaskException(
                SqlTaskErrorCode.SQL_TASK_START_NODE_INVALID,
                "missing start node"
        ));
        assertThat(invalidStartNode.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);

        var configError = handler.handleSqlTaskException(new SqlTaskException(
                SqlTaskErrorCode.SQL_TASK_DAG_INVALID,
                "cycle"
        ));
        assertThat(configError.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
        assertThat(configError.getBody()).isEqualTo(Map.of(
                "code", "SQL_TASK_DAG_INVALID",
                "message", "cycle"
        ));
    }

    private SqlTaskExecutionResult result() {
        return new SqlTaskExecutionResult(
                "run-1",
                SqlTaskRunStatus.SUCCESS,
                "classpath:sql/tasks/codeforces-warehouse-refresh.yml",
                null,
                null,
                NOW,
                NOW,
                0L,
                List.of(SqlTaskNodeResult.success(
                        new com.custacm.platform.common.sqltask.SqlTaskDefinition(
                                "codeforces.dwd.submission",
                                "Refresh DWD",
                                "classpath:sql/dwd/upsert_dwd_codeforces__submission.sql",
                                List.of(),
                                Duration.ofSeconds(60)
                        ),
                        NOW,
                        NOW,
                        1000
                ))
        );
    }
}
