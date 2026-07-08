package com.custacm.platform.trainingdata.common.collector.job;

public interface OjWarehouseRefreshHandler {
    String ojName();

    OjSubmissionCollectionJobRefreshResult refresh(String batchId);
}
