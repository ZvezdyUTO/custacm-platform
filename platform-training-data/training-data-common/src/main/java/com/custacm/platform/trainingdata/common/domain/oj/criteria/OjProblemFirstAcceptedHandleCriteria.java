package com.custacm.platform.trainingdata.common.domain.oj.criteria;

import com.custacm.platform.trainingdata.common.domain.oj.value.OjNames;

import java.time.LocalDateTime;

public record OjProblemFirstAcceptedHandleCriteria(
        String ojName,
        String problemKey,
        LocalDateTime firstAcceptedFromUtcPlus8,
        LocalDateTime firstAcceptedToUtcPlus8
) {
    public OjProblemFirstAcceptedHandleCriteria(
            String problemKey,
            LocalDateTime firstAcceptedFromUtcPlus8,
            LocalDateTime firstAcceptedToUtcPlus8
    ) {
        this(OjNames.CODEFORCES, problemKey, firstAcceptedFromUtcPlus8, firstAcceptedToUtcPlus8);
    }
}
