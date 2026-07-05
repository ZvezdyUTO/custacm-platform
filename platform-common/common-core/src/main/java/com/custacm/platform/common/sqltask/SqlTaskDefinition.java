package com.custacm.platform.common.sqltask;

import java.time.Duration;
import java.util.List;

public record SqlTaskDefinition(
        String id,
        String description,
        String sqlLocation,
        List<String> dependsOn,
        Duration timeout
) {
    public SqlTaskDefinition {
        id = requireText(id, "task id");
        description = description == null ? "" : description.trim();
        sqlLocation = requireText(sqlLocation, "task sqlLocation");
        dependsOn = dependsOn == null ? List.of() : List.copyOf(dependsOn);
        timeout = timeout == null ? Duration.ZERO : timeout;
        if (timeout.isNegative()) {
            throw new SqlTaskException(SqlTaskErrorCode.SQL_TASK_CONFIG_INVALID, "task timeout must not be negative");
        }
    }

    private static String requireText(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new SqlTaskException(SqlTaskErrorCode.SQL_TASK_CONFIG_INVALID, fieldName + " must not be blank");
        }
        return value.trim();
    }
}
