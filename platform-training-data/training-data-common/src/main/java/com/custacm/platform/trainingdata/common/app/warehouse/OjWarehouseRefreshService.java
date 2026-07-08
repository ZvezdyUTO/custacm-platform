package com.custacm.platform.trainingdata.common.app.warehouse;

import com.custacm.platform.common.sqltask.SqlTaskExecutionRequest;
import com.custacm.platform.common.sqltask.SqlTaskExecutionResult;
import com.custacm.platform.common.sqltask.SqlTaskRunner;
import com.custacm.platform.trainingdata.common.domain.oj.model.OjWarehouseRefreshInterval;
import com.custacm.platform.trainingdata.common.domain.oj.repo.OjWarehouseRefreshIntervalRepository;

import java.sql.Date;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

import static com.custacm.platform.trainingdata.common.support.Texts.requireText;

public class OjWarehouseRefreshService {
    private final SqlTaskRunner sqlTaskRunner;
    private final OjWarehouseRefreshIntervalRepository intervalRepository;
    private final String manifestLocation;
    private final String missingIntervalMessage;

    public OjWarehouseRefreshService(
            SqlTaskRunner sqlTaskRunner,
            OjWarehouseRefreshIntervalRepository intervalRepository,
            String manifestLocation,
            String missingIntervalMessage
    ) {
        this.sqlTaskRunner = Objects.requireNonNull(sqlTaskRunner, "sqlTaskRunner must not be null");
        this.intervalRepository = Objects.requireNonNull(intervalRepository, "intervalRepository must not be null");
        this.manifestLocation = requireText(manifestLocation, "manifestLocation");
        this.missingIntervalMessage = requireText(missingIntervalMessage, "missingIntervalMessage");
    }

    public SqlTaskExecutionResult refresh(String batchId, String startFromTaskId) {
        String normalizedBatchId = requireText(batchId, "batchId");
        String normalizedStartFromTaskId = normalizeOptionalText(startFromTaskId);
        OjWarehouseRefreshInterval refreshInterval = intervalRepository.findBatchDateInterval(normalizedBatchId)
                .orElseThrow(() -> new IllegalArgumentException(missingIntervalMessage));
        return sqlTaskRunner.execute(executionRequest(
                normalizedBatchId,
                refreshInterval,
                normalizedStartFromTaskId
        ));
    }

    private SqlTaskExecutionRequest executionRequest(
            String batchId,
            OjWarehouseRefreshInterval refreshInterval,
            String startFromTaskId
    ) {
        Map<String, Object> parameters = new LinkedHashMap<>();
        parameters.put("batchId", batchId);
        parameters.put("refreshFromDateUtcPlus8", Date.valueOf(refreshInterval.fromDateUtcPlus8()));
        parameters.put("refreshToDateUtcPlus8", Date.valueOf(refreshInterval.toDateUtcPlus8()));
        return new SqlTaskExecutionRequest(
                manifestLocation,
                parameters,
                startFromTaskId
        );
    }

    private static String normalizeOptionalText(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
