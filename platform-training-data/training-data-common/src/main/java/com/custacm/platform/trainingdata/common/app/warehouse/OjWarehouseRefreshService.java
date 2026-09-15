package com.custacm.platform.trainingdata.common.app.warehouse;

import com.custacm.platform.common.sqltask.SqlTaskExecutionRequest;
import com.custacm.platform.common.sqltask.SqlTaskExecutionResult;
import com.custacm.platform.common.sqltask.SqlTaskRunStatus;
import com.custacm.platform.common.sqltask.SqlTaskRunner;
import com.custacm.platform.trainingdata.common.domain.oj.model.OjWarehouseRefreshInterval;
import com.custacm.platform.trainingdata.common.domain.oj.repo.OjWarehouseRefreshIntervalRepository;

import java.sql.Date;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

import static com.custacm.platform.trainingdata.common.support.Texts.requireText;

public class OjWarehouseRefreshService {
    private final SqlTaskRunner sqlTaskRunner;
    private final OjWarehouseRefreshIntervalRepository intervalRepository;
    private final String manifestLocation;
    private final String missingIntervalMessage;
    // Each OJ owns one service. Retain only the union of unfinished work, not a growing job history.
    private PendingRefresh pendingRefresh;

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

    public synchronized SqlTaskExecutionResult refresh(String batchId, String startFromTaskId) {
        String normalizedBatchId = requireText(batchId, "batchId");
        String normalizedStartFromTaskId = normalizeOptionalText(startFromTaskId);
        OjWarehouseRefreshInterval refreshInterval = pendingRefresh != null
                && pendingRefresh.batchId().equals(normalizedBatchId)
                ? pendingRefresh.interval()
                : intervalRepository.findBatchDateInterval(normalizedBatchId)
                        .orElseThrow(() -> new IllegalArgumentException(missingIntervalMessage));
        if (pendingRefresh != null) {
            refreshInterval = coveringInterval(pendingRefresh.interval(), refreshInterval);
            // A retry may also cover earlier failed batches, so all warehouse layers must be rebuilt.
            normalizedStartFromTaskId = null;
        }
        return execute(new PendingRefresh(normalizedBatchId, refreshInterval), normalizedStartFromTaskId);
    }

    public synchronized SqlTaskExecutionResult refreshLatest(String startFromTaskId) {
        String latestBatchId = intervalRepository.findLatestBatchId()
                .orElseThrow(() -> new IllegalArgumentException(missingIntervalMessage));
        return refresh(latestBatchId, startFromTaskId);
    }

    public synchronized Optional<SqlTaskExecutionResult> refreshPending() {
        return pendingRefresh == null ? Optional.empty() : Optional.of(execute(pendingRefresh, null));
    }

    private SqlTaskExecutionResult execute(PendingRefresh refresh, String startFromTaskId) {
        // Capture before executing: later nodes can fail after earlier nodes committed their changes.
        pendingRefresh = refresh;
        SqlTaskExecutionResult result = sqlTaskRunner.execute(executionRequest(
                refresh.batchId(), refresh.interval(), startFromTaskId));
        if (result.status() == SqlTaskRunStatus.SUCCESS) {
            pendingRefresh = null;
        }
        return result;
    }

    private static OjWarehouseRefreshInterval coveringInterval(
            OjWarehouseRefreshInterval left,
            OjWarehouseRefreshInterval right
    ) {
        return new OjWarehouseRefreshInterval(
                left.fromDateUtcPlus8().isBefore(right.fromDateUtcPlus8())
                        ? left.fromDateUtcPlus8() : right.fromDateUtcPlus8(),
                left.toDateUtcPlus8().isAfter(right.toDateUtcPlus8())
                        ? left.toDateUtcPlus8() : right.toDateUtcPlus8()
        );
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

    private record PendingRefresh(String batchId, OjWarehouseRefreshInterval interval) {
    }
}
