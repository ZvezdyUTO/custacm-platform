package com.custacm.platform.trainingdata.common.app.query.result;

import java.time.LocalDateTime;
import java.util.List;

public record OjProblemFirstAcceptedHandleReport(
        String problemKey,
        int acceptedHandleCount,
        List<OjFirstAcceptedHandle> acceptedHandles
) {
    public record OjFirstAcceptedHandle(
            String studentIdentity,
            String handle,
            LocalDateTime firstAcceptedAtUtcPlus8
    ) {
    }
}
