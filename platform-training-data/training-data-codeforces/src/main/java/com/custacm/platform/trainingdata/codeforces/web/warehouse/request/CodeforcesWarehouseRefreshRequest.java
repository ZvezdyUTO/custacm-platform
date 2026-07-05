package com.custacm.platform.trainingdata.codeforces.web.warehouse.request;

public record CodeforcesWarehouseRefreshRequest(
        String batchId,
        String startFromTaskId
) {
}
