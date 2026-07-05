package com.custacm.platform.trainingdata.codeforces.web.warehouse;

import com.custacm.platform.common.sqltask.SqlTaskExecutionResult;
import com.custacm.platform.trainingdata.codeforces.app.warehouse.CodeforcesWarehouseRefreshService;
import com.custacm.platform.trainingdata.codeforces.web.warehouse.request.CodeforcesWarehouseRefreshRequest;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class CodeforcesWarehouseRefreshController {
    private final CodeforcesWarehouseRefreshService refreshService;

    public CodeforcesWarehouseRefreshController(CodeforcesWarehouseRefreshService refreshService) {
        this.refreshService = refreshService;
    }

    @PostMapping("/api/training-data/admin/codeforces/warehouse:refresh")
    public SqlTaskExecutionResult refresh(
            @RequestBody CodeforcesWarehouseRefreshRequest request
    ) {
        if (request == null) {
            throw new IllegalArgumentException("request body must not be empty");
        }
        return refreshService.refresh(request.batchId(), request.startFromTaskId());
    }
}
