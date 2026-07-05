package com.custacm.platform.common.sqltask;

import java.time.Instant;
import java.util.List;

public record SqlTaskExecutionResult(
        String runId,
        SqlTaskRunStatus status,
        String manifestLocation,
        String startFromTaskId,
        String failedTaskId,
        Instant startedAt,
        Instant finishedAt,
        long durationMillis,
        List<SqlTaskNodeResult> tasks
) {
    public SqlTaskExecutionResult {
        tasks = List.copyOf(tasks);
    }
}
