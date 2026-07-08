package com.custacm.platform.trainingdata.common.collector.job;

import com.custacm.platform.common.sqltask.SqlTaskExecutionResult;
import com.custacm.platform.common.sqltask.SqlTaskRunStatus;
import com.custacm.platform.trainingdata.common.app.warehouse.OjWarehouseRefreshService;
import com.custacm.platform.trainingdata.common.domain.oj.value.OjNames;

import java.util.Objects;

public class SqlTaskOjWarehouseRefreshHandler implements OjWarehouseRefreshHandler {
    private final String ojName;
    private final OjWarehouseRefreshService refreshService;

    public SqlTaskOjWarehouseRefreshHandler(String ojName, OjWarehouseRefreshService refreshService) {
        this.ojName = OjNames.normalize(ojName);
        this.refreshService = Objects.requireNonNull(refreshService, "refreshService must not be null");
    }

    @Override
    public String ojName() {
        return ojName;
    }

    @Override
    public OjSubmissionCollectionJobRefreshResult refresh(String batchId) {
        SqlTaskExecutionResult result = refreshService.refresh(batchId, null);
        return new OjSubmissionCollectionJobRefreshResult(
                result.status() == SqlTaskRunStatus.SUCCESS
                        ? OjSubmissionCollectionJobRefreshStatus.SUCCESS
                        : OjSubmissionCollectionJobRefreshStatus.FAILED,
                result.status().name()
        );
    }
}
