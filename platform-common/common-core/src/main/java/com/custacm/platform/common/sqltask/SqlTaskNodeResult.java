package com.custacm.platform.common.sqltask;

import java.time.Instant;

public record SqlTaskNodeResult(
        String taskId,
        String description,
        String sqlLocation,
        SqlTaskNodeStatus status,
        Instant startedAt,
        Instant finishedAt,
        long durationMillis,
        int affectedRows,
        String errorCode,
        String message
) {
    public static SqlTaskNodeResult success(
            SqlTaskDefinition definition,
            Instant startedAt,
            Instant finishedAt,
            int affectedRows
    ) {
        return new SqlTaskNodeResult(
                definition.id(),
                definition.description(),
                definition.sqlLocation(),
                SqlTaskNodeStatus.SUCCESS,
                startedAt,
                finishedAt,
                millisBetween(startedAt, finishedAt),
                affectedRows,
                null,
                null
        );
    }

    public static SqlTaskNodeResult failed(
            SqlTaskDefinition definition,
            Instant startedAt,
            Instant finishedAt,
            SqlTaskErrorCode errorCode,
            String message
    ) {
        return new SqlTaskNodeResult(
                definition.id(),
                definition.description(),
                definition.sqlLocation(),
                SqlTaskNodeStatus.FAILED,
                startedAt,
                finishedAt,
                millisBetween(startedAt, finishedAt),
                0,
                errorCode.name(),
                message
        );
    }

    public static SqlTaskNodeResult skipped(SqlTaskDefinition definition, String message) {
        return new SqlTaskNodeResult(
                definition.id(),
                definition.description(),
                definition.sqlLocation(),
                SqlTaskNodeStatus.SKIPPED,
                null,
                null,
                0L,
                0,
                null,
                message
        );
    }

    private static long millisBetween(Instant startedAt, Instant finishedAt) {
        return Math.max(0L, finishedAt.toEpochMilli() - startedAt.toEpochMilli());
    }
}
