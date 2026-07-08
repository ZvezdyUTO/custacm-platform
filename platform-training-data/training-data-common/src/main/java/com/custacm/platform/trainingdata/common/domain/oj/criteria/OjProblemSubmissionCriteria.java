package com.custacm.platform.trainingdata.common.domain.oj.criteria;

import com.custacm.platform.trainingdata.common.domain.oj.value.OjNames;

import java.time.LocalDateTime;

public record OjProblemSubmissionCriteria(
        String ojName,
        String problemKey,
        LocalDateTime submittedFromUtcPlus8,
        LocalDateTime submittedToUtcPlus8,
        int limit,
        long offset
) {
    public OjProblemSubmissionCriteria(
            String problemKey,
            LocalDateTime submittedFromUtcPlus8,
            LocalDateTime submittedToUtcPlus8,
            int limit,
            long offset
    ) {
        this(OjNames.CODEFORCES, problemKey, submittedFromUtcPlus8, submittedToUtcPlus8, limit, offset);
    }
}
