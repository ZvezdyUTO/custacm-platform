package com.custacm.platform.common.sqltask;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.DefaultTransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class SqlTaskRunner {
    private static final Logger log = LoggerFactory.getLogger(SqlTaskRunner.class);

    private final NamedParameterJdbcTemplate jdbcTemplate;
    private final PlatformTransactionManager transactionManager;
    private final ResourceLoader resourceLoader;
    private final YamlSqlTaskManifestLoader manifestLoader;

    public SqlTaskRunner(
            NamedParameterJdbcTemplate jdbcTemplate,
            PlatformTransactionManager transactionManager,
            ResourceLoader resourceLoader
    ) {
        this.jdbcTemplate = jdbcTemplate;
        this.transactionManager = transactionManager;
        this.resourceLoader = resourceLoader;
        this.manifestLoader = new YamlSqlTaskManifestLoader(resourceLoader);
    }

    public SqlTaskExecutionResult execute(SqlTaskExecutionRequest request) {
        String runId = UUID.randomUUID().toString();
        Instant startedAt = Instant.now();
        log.info(
                "SQL task run started, runId={}, manifestLocation={}, startFromTaskId={}",
                runId,
                request.manifestLocation(),
                request.startFromTaskId()
        );

        SqlTaskManifest manifest = manifestLoader.load(request.manifestLocation());
        SqlTaskGraph graph = SqlTaskGraph.from(manifest.tasks());
        List<SqlTaskDefinition> executionPlan = graph.executionPlan(request.startFromTaskId());
        List<SqlTaskNodeResult> nodeResults = new ArrayList<>();

        for (int index = 0; index < executionPlan.size(); index++) {
            SqlTaskDefinition task = executionPlan.get(index);
            Instant taskStartedAt = Instant.now();
            log.info("SQL task node started, runId={}, taskId={}", runId, task.id());
            try {
                int affectedRows = executeTask(task, request);
                Instant taskFinishedAt = Instant.now();
                nodeResults.add(SqlTaskNodeResult.success(task, taskStartedAt, taskFinishedAt, affectedRows));
                log.info(
                        "SQL task node succeeded, runId={}, taskId={}, affectedRows={}, durationMillis={}",
                        runId,
                        task.id(),
                        affectedRows,
                        taskFinishedAt.toEpochMilli() - taskStartedAt.toEpochMilli()
                );
            } catch (SqlTaskException ex) {
                if (ex.errorCode() != SqlTaskErrorCode.SQL_TASK_SQL_EXECUTION_FAILED) {
                    throw ex;
                }
                return failedResult(runId, request, startedAt, executionPlan, nodeResults, index, task, taskStartedAt, ex);
            } catch (RuntimeException ex) {
                return failedResult(runId, request, startedAt, executionPlan, nodeResults, index, task, taskStartedAt, ex);
            }
        }
        Instant finishedAt = Instant.now();
        log.info(
                "SQL task run succeeded, runId={}, manifestLocation={}, executedTaskCount={}, durationMillis={}",
                runId,
                request.manifestLocation(),
                nodeResults.size(),
                finishedAt.toEpochMilli() - startedAt.toEpochMilli()
        );
        return result(runId, SqlTaskRunStatus.SUCCESS, request, null, startedAt, finishedAt, nodeResults);
    }

    private int executeTask(SqlTaskDefinition task, SqlTaskExecutionRequest request) {
        TransactionTemplate transactionTemplate = new TransactionTemplate(transactionManager, transactionDefinition(task));
        return transactionTemplate.execute(status -> executeStatements(task, request));
    }

    private DefaultTransactionDefinition transactionDefinition(SqlTaskDefinition task) {
        DefaultTransactionDefinition definition = new DefaultTransactionDefinition();
        definition.setName("sql-task:" + task.id());
        definition.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRED);
        if (!task.timeout().isZero()) {
            definition.setTimeout(Math.toIntExact(task.timeout().toSeconds()));
        }
        return definition;
    }

    private int executeStatements(SqlTaskDefinition task, SqlTaskExecutionRequest request) {
        String sqlScript = loadSqlScript(task);
        List<String> statements = SqlScriptSplitter.split(sqlScript);
        if (statements.isEmpty()) {
            throw new SqlTaskException(
                    SqlTaskErrorCode.SQL_TASK_CONFIG_INVALID,
                    "sql task script has no executable statements: " + task.sqlLocation()
            );
        }
        int affectedRows = 0;
        for (String statement : statements) {
            int statementRows = jdbcTemplate.update(statement, request.parameters());
            affectedRows += Math.max(statementRows, 0);
        }
        return affectedRows;
    }

    private SqlTaskExecutionResult failedResult(
            String runId,
            SqlTaskExecutionRequest request,
            Instant runStartedAt,
            List<SqlTaskDefinition> executionPlan,
            List<SqlTaskNodeResult> nodeResults,
            int failedTaskIndex,
            SqlTaskDefinition failedTask,
            Instant taskStartedAt,
            RuntimeException ex
    ) {
        Instant taskFinishedAt = Instant.now();
        SqlTaskErrorCode errorCode = SqlTaskErrorCode.SQL_TASK_SQL_EXECUTION_FAILED;
        nodeResults.add(SqlTaskNodeResult.failed(failedTask, taskStartedAt, taskFinishedAt, errorCode, ex.getMessage()));
        for (SqlTaskDefinition skippedTask : executionPlan.subList(failedTaskIndex + 1, executionPlan.size())) {
            nodeResults.add(SqlTaskNodeResult.skipped(skippedTask, "Skipped because task failed: " + failedTask.id()));
        }
        log.error(
                "SQL task node failed, errorCode={}, runId={}, taskId={}, sqlLocation={}, durationMillis={}",
                errorCode,
                runId,
                failedTask.id(),
                failedTask.sqlLocation(),
                taskFinishedAt.toEpochMilli() - taskStartedAt.toEpochMilli(),
                ex
        );
        return result(
                runId,
                SqlTaskRunStatus.FAILED,
                request,
                failedTask.id(),
                runStartedAt,
                Instant.now(),
                nodeResults
        );
    }

    private String loadSqlScript(SqlTaskDefinition task) {
        Resource resource = resourceLoader.getResource(task.sqlLocation());
        if (!resource.exists()) {
            throw new SqlTaskException(
                    SqlTaskErrorCode.SQL_TASK_RESOURCE_UNREADABLE,
                    "sql task script does not exist: " + task.sqlLocation()
            );
        }
        try {
            return resource.getContentAsString(StandardCharsets.UTF_8);
        } catch (IOException ex) {
            throw new SqlTaskException(
                    SqlTaskErrorCode.SQL_TASK_RESOURCE_UNREADABLE,
                    "failed to read sql task script: " + task.sqlLocation(),
                    ex
            );
        }
    }

    private SqlTaskExecutionResult result(
            String runId,
            SqlTaskRunStatus status,
            SqlTaskExecutionRequest request,
            String failedTaskId,
            Instant startedAt,
            Instant finishedAt,
            List<SqlTaskNodeResult> nodeResults
    ) {
        return new SqlTaskExecutionResult(
                runId,
                status,
                request.manifestLocation(),
                request.startFromTaskId(),
                failedTaskId,
                startedAt,
                finishedAt,
                Math.max(0L, finishedAt.toEpochMilli() - startedAt.toEpochMilli()),
                nodeResults
        );
    }
}
