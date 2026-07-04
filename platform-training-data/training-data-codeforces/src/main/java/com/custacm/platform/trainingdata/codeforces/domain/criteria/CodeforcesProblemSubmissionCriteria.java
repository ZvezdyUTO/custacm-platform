package com.custacm.platform.trainingdata.codeforces.domain.criteria;

import java.time.LocalDateTime;

public record CodeforcesProblemSubmissionCriteria(
        String problemKey,
        LocalDateTime submittedFromUtcPlus8,
        LocalDateTime submittedToUtcPlus8
) {
}
