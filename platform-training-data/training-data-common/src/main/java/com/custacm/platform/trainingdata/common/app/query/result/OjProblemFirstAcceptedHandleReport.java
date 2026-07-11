package com.custacm.platform.trainingdata.common.app.query.result;

import java.time.LocalDateTime;
import java.util.List;

public record OjProblemFirstAcceptedHandleReport(
        String problemKey,
        int acceptedHandleCount,
        int page,
        int limit,
        long total,
        long totalPages,
        boolean hasMore,
        List<OjFirstAcceptedHandle> acceptedHandles
) {
    public record OjFirstAcceptedHandle(
            String username,
            String handle,
            LocalDateTime firstAcceptedAtUtcPlus8
    ) {
    }
}
