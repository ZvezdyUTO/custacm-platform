package com.custacm.platform.trainingdata.common.domain.oj.repo;

import com.custacm.platform.trainingdata.common.domain.oj.model.OjWarehouseRefreshInterval;

import java.util.Optional;

public interface OjWarehouseRefreshIntervalRepository {
    Optional<OjWarehouseRefreshInterval> findBatchDateInterval(String batchId);
}
