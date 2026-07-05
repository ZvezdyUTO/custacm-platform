package com.custacm.platform.common.sqltask;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

public record SqlTaskExecutionRequest(
        String manifestLocation,
        Map<String, ?> parameters,
        String startFromTaskId
) {
    public SqlTaskExecutionRequest {
        if (manifestLocation == null || manifestLocation.isBlank()) {
            throw new SqlTaskException(
                    SqlTaskErrorCode.SQL_TASK_CONFIG_INVALID,
                    "sql task manifestLocation must not be blank"
            );
        }
        manifestLocation = manifestLocation.trim();
        parameters = parameters == null ? Map.of() : Collections.unmodifiableMap(new LinkedHashMap<>(parameters));
        startFromTaskId = startFromTaskId == null || startFromTaskId.isBlank() ? null : startFromTaskId.trim();
    }
}
