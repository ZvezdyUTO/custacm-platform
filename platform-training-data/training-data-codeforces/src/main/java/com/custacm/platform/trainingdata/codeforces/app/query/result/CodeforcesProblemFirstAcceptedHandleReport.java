package com.custacm.platform.trainingdata.codeforces.app.query.result;

import java.time.LocalDateTime;
import java.util.List;

public record CodeforcesProblemFirstAcceptedHandleReport(
        String problemKey,
        int acceptedHandleCount,
        List<CodeforcesFirstAcceptedHandle> acceptedHandles
) {
    public record CodeforcesFirstAcceptedHandle(
            String studentIdentity,
            String authorHandle,
            LocalDateTime firstAcceptedAtUtcPlus8
    ) {
    }
}
