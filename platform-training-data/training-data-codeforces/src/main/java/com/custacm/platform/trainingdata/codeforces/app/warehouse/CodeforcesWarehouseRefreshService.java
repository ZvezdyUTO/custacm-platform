package com.custacm.platform.trainingdata.codeforces.app.warehouse;

import com.custacm.platform.common.sqltask.SqlTaskExecutionRequest;
import com.custacm.platform.common.sqltask.SqlTaskExecutionResult;
import com.custacm.platform.common.sqltask.SqlTaskRunner;
import com.custacm.platform.trainingdata.codeforces.domain.model.CodeforcesWarehouseRefreshInterval;
import com.custacm.platform.trainingdata.codeforces.domain.repo.CodeforcesWarehouseRefreshIntervalRepository;

import java.sql.Date;
import java.util.LinkedHashMap;
import java.util.Map;

public class CodeforcesWarehouseRefreshService {
    private static final String MANIFEST_LOCATION = "classpath:sql/tasks/codeforces-warehouse-refresh.yml";

    private final SqlTaskRunner sqlTaskRunner;
    private final CodeforcesWarehouseRefreshIntervalRepository intervalRepository;

    public CodeforcesWarehouseRefreshService(
            SqlTaskRunner sqlTaskRunner,
            CodeforcesWarehouseRefreshIntervalRepository intervalRepository
    ) {
        this.sqlTaskRunner = sqlTaskRunner;
        this.intervalRepository = intervalRepository;
    }

    public SqlTaskExecutionResult refresh(String batchId, String startFromTaskId) {
        String normalizedBatchId = requireText(batchId, "batchId");
        String normalizedStartFromTaskId = normalizeOptionalText(startFromTaskId);
        CodeforcesWarehouseRefreshInterval refreshInterval = intervalRepository.findBatchDateInterval(normalizedBatchId)
                .orElseThrow(() -> new IllegalArgumentException(
                        "batchId has no Codeforces submissions with creationTimeSeconds"
                ));
        return sqlTaskRunner.execute(executionRequest(
                normalizedBatchId,
                refreshInterval,
                normalizedStartFromTaskId
        ));
    }

    private SqlTaskExecutionRequest executionRequest(
            String batchId,
            CodeforcesWarehouseRefreshInterval refreshInterval,
            String startFromTaskId
    ) {
        Map<String, Object> parameters = new LinkedHashMap<>();
        parameters.put("batchId", batchId);
        parameters.put(
                "refreshFromDateUtcPlus8",
                Date.valueOf(refreshInterval.fromDateUtcPlus8())
        );
        parameters.put(
                "refreshToDateUtcPlus8",
                Date.valueOf(refreshInterval.toDateUtcPlus8())
        );
        return new SqlTaskExecutionRequest(
                MANIFEST_LOCATION,
                parameters,
                startFromTaskId
        );
    }

    private String requireText(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(fieldName + " must not be blank");
        }
        return value.trim();
    }

    private String normalizeOptionalText(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
