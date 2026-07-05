package com.custacm.platform.trainingdata.codeforces.domain.repo;

import com.custacm.platform.trainingdata.codeforces.domain.model.CodeforcesWarehouseRefreshInterval;

import java.util.Optional;

public interface CodeforcesWarehouseRefreshIntervalRepository {
    Optional<CodeforcesWarehouseRefreshInterval> findBatchDateInterval(String batchId);
}
